package com.p2p.chunking;

import com.p2p.core.ChunkInfo;
import com.p2p.core.Manifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Generates manifests for files.
 *
 * A manifest is like a table of contents that describes:
 * - What the file is (name, size)
 * - How it's divided (chunk size, number of chunks)
 * - How to verify each piece (chunk hashes)
 */
public class ManifestGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ManifestGenerator.class);

    private final FileChunker chunker;

    public ManifestGenerator() {
        this.chunker = new FileChunker();
    }

    public ManifestGenerator(FileChunker chunker) {
        this.chunker = chunker;
    }

    /**
     * Generates a manifest for a file.
     *
     * Steps:
     * 1. Chunk the file and get chunk info (index, hash, size)
     * 2. Create a manifest with file metadata
     * 3. Calculate a unique file ID (hash of the manifest data)
     *
     * @param file The file to generate a manifest for
     * @return A complete Manifest object
     */
    public Manifest generateManifest(File file) throws IOException {
        logger.info("Generating manifest for file: {}", file.getName());

        // Get chunk information
        List<ChunkInfo> chunks = chunker.chunkFile(file);

        // Generate a unique file ID
        // We use hash of (filename + filesize + chunk count) as a simple file ID
        String fileId = generateFileId(file, chunks);

        // Create manifest
        Manifest manifest = new Manifest(
                fileId,
                file.getName(),
                file.length(),
                chunker.getChunkSize(),
                chunks
        );

        logger.info("Manifest generated: fileId={}, chunks={}",
                fileId.substring(0, 8) + "...", chunks.size());

        return manifest;
    }

    /**
     * Generates a unique file ID based on file metadata.
     *
     * This creates a unique identifier by hashing:
     * - Filename
     * - File size
     * - All chunk hashes concatenated
     *
     * This ensures that:
     * - Same file = same ID
     * - Different file = different ID
     * - Modified file = different ID
     */
    private String generateFileId(File file, List<ChunkInfo> chunks) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Add filename
            digest.update(file.getName().getBytes());

            // Add file size
            digest.update(longToBytes(file.length()));

            // Add all chunk hashes
            for (ChunkInfo chunk : chunks) {
                digest.update(chunk.getHash().getBytes());
            }

            byte[] hash = digest.digest();
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Converts a long to bytes.
     */
    private byte[] longToBytes(long value) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return result;
    }

    /**
     * Converts bytes to hexadecimal string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
