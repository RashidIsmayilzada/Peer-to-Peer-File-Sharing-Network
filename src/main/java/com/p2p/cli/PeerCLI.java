package com.p2p.cli;

import com.p2p.chunking.FileChunker;
import com.p2p.chunking.ManifestGenerator;
import com.p2p.core.Manifest;
import com.p2p.network.DownloadManager;
import com.p2p.network.PeerServer;
import com.p2p.storage.ChunkStorage;
import com.p2p.storage.ManifestStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;

/**
 * Main entry point for the P2P file sharing application.
 *
 * This is like the conductor of an orchestra - it:
 * - Parses what you want to do (seed or download)
 * - Starts the right components (server, client)
 * - Coordinates everything
 */
public class PeerCLI {
    private static final Logger logger = LoggerFactory.getLogger(PeerCLI.class);

    public static void main(String[] args) {
        logger.info("Starting P2P File Sharing Network...");

        try {
            // Parse command-line arguments
            CLIArguments cliArgs = CLIParser.parse(args);
            logger.info("Mode: {}, Port: {}", cliArgs.getMode(), cliArgs.getPort());

            if (cliArgs.getMode() == CLIArguments.Mode.SEED) {
                runSeedMode(cliArgs);
            } else {
                runDownloadMode(cliArgs);
            }

        } catch (IllegalArgumentException e) {
            logger.error("Invalid arguments: {}", e.getMessage());
            System.err.println("Error: " + e.getMessage());
            System.err.println("Use --help for usage information");
            System.exit(1);
        } catch (Exception e) {
            logger.error("Fatal error", e);
            System.err.println("Fatal error: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Runs in SEED mode: shares a file with the network.
     *
     * Steps:
     * 1. Chunk the file and generate manifest
     * 2. Store chunks and manifest
     * 3. Start TCP server to listen for connections
     * 4. Wait for other peers to connect and request chunks
     */
    private static void runSeedMode(CLIArguments args) throws Exception {
        logger.info("Running in SEED mode for file: {}", args.getSeedFile());

        // Verify file exists
        File file = new File(args.getSeedFile());
        if (!file.exists()) {
            throw new IllegalArgumentException("File not found: " + args.getSeedFile());
        }

        // Initialize storage
        ChunkStorage chunkStorage = new ChunkStorage();
        ManifestStorage manifestStorage = new ManifestStorage();

        // Generate manifest
        logger.info("Generating manifest and chunking file...");
        ManifestGenerator manifestGenerator = new ManifestGenerator();
        Manifest manifest = manifestGenerator.generateManifest(file);

        // Store manifest
        manifestStorage.storeManifest(manifest);
        logger.info("Manifest stored: {}", manifest.getFileId());

        // Store all chunks
        logger.info("Storing {} chunks...", manifest.getChunkCount());
        FileChunker chunker = new FileChunker();
        for (int i = 0; i < manifest.getChunkCount(); i++) {
            byte[] chunkData = chunker.readChunk(file, i);
            String chunkHash = manifest.getChunk(i).getHash();
            chunkStorage.storeChunk(chunkHash, chunkData);

            if ((i + 1) % 10 == 0 || i == manifest.getChunkCount() - 1) {
                logger.info("Stored {}/{} chunks", i + 1, manifest.getChunkCount());
            }
        }

        logger.info("File ready for sharing!");
        logger.info("File ID: {}", manifest.getFileId());
        logger.info("Filename: {}", manifest.getFilename());
        logger.info("Size: {} bytes ({} chunks)", manifest.getFileSize(), manifest.getChunkCount());
        System.out.println("\n=== FILE READY FOR SHARING ===");
        System.out.println("File ID: " + manifest.getFileId());
        System.out.println("Filename: " + manifest.getFilename());
        System.out.println("Share this File ID with others to let them download!");
        System.out.println("================================\n");

        // Start the server with storage
        PeerServer server = new PeerServer(args.getPort(), manifestStorage, chunkStorage);
        server.addAvailableFile(manifest.getFileId());

        // Add shutdown hook for graceful cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown signal received");
            server.shutdown();
        }));

        // Start server (blocks until shutdown)
        server.start();
    }

    /**
     * Runs in DOWNLOAD mode: downloads a file from the network.
     *
     * Steps:
     * 1. Start TCP server (so we can also share what we download)
     * 2. Connect to bootstrap peer
     * 3. Request manifest for the file
     * 4. Download chunks from available peers
     * 5. Reassemble and verify the file
     */
    private static void runDownloadMode(CLIArguments args) throws Exception {
        logger.info("Running in DOWNLOAD mode for file: {}", args.getDownloadFileId());
        logger.info("Bootstrap peer: {}", args.getBootstrap());

        // Parse bootstrap address
        String[] parts = args.getBootstrap().split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid bootstrap format. Use host:port");
        }
        String bootstrapHost = parts[0];
        int bootstrapPort = Integer.parseInt(parts[1]);

        // Initialize storage
        ChunkStorage chunkStorage = new ChunkStorage();
        ManifestStorage manifestStorage = new ManifestStorage();

        // Start our own server first (so we can also share what we download)
        PeerServer server = new PeerServer(args.getPort(), manifestStorage, chunkStorage);
        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (InterruptedException e) {
                logger.error("Server interrupted", e);
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // Give server a moment to start
        Thread.sleep(1000);

        // Create download directory
        File downloadDir = Paths.get("downloads").toFile();
        downloadDir.mkdirs();

        // Use DownloadManager to download the file
        DownloadManager downloadManager = new DownloadManager(
                server.getPeerId(),
                args.getPort(),
                chunkStorage
        );

        // Download to temporary file first
        File tempFile = new File(downloadDir, args.getDownloadFileId().substring(0, 8) + ".tmp");

        logger.info("Downloading file to temporary location: {}", tempFile.getAbsolutePath());

        try {
            // Download the file and get manifest with original filename
            Manifest manifest = downloadManager.downloadFile(
                    args.getDownloadFileId(),
                    bootstrapHost,
                    bootstrapPort,
                    tempFile
            );

            // Rename to original filename
            File finalFile = new File(downloadDir, manifest.getFilename());
            if (finalFile.exists()) {
                logger.warn("File already exists, will overwrite: {}", finalFile.getName());
                finalFile.delete();
            }
            tempFile.renameTo(finalFile);

            // Store manifest for future sharing
            // (DownloadManager already stored chunks)
            manifestStorage.storeManifest(manifest);

            logger.info("Download complete!");
            System.out.println("\n=== DOWNLOAD COMPLETE ===");
            System.out.println("File saved to: " + finalFile.getAbsolutePath());
            System.out.println("Original filename: " + manifest.getFilename());
            System.out.println("File size: " + finalFile.length() + " bytes");
            System.out.println("You can now share this file with others!");
            System.out.println("=========================\n");

            // Add file to available files
            server.addAvailableFile(args.getDownloadFileId());

            // Keep server running to share the downloaded file
            logger.info("Server will continue running to share the downloaded file...");
            logger.info("Press Ctrl+C to stop");

            // Wait for shutdown signal
            Thread.currentThread().join();

        } catch (Exception e) {
            logger.error("Download failed", e);
            throw e;
        } finally {
            server.shutdown();
        }
    }
}
