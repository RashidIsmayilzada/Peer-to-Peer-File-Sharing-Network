package com.p2p.storage;

import com.p2p.core.ChunkInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Manages storage and retrieval of file chunks.
 *
 * Think of this as a warehouse that:
 * - Stores boxes (chunks) in organized shelves
 * - Each box has a label (hash)
 * - Can quickly find and retrieve any box
 * - Verifies boxes haven't been damaged
 *
 * Storage structure:
 * .chunks/
 *   ab/
 *     abc123def456...  (chunk file named by hash)
 *   cd/
 *     cde789fgh012...
 */
public class ChunkStorage {
    private static final Logger logger = LoggerFactory.getLogger(ChunkStorage.class);

    private final Path storageRoot;

    /**
     * Creates a ChunkStorage with default storage directory ".chunks"
     */
    public ChunkStorage() {
        this(Paths.get(".chunks"));
    }

    /**
     * Creates a ChunkStorage with custom storage directory.
     *
     * @param storageRoot Root directory for chunk storage
     */
    public ChunkStorage(Path storageRoot) {
        this.storageRoot = storageRoot;
        ensureStorageExists();
    }

    /**
     * Stores a chunk with the given hash.
     *
     * The chunk is stored in a two-level directory structure:
     * - First 2 chars of hash = subdirectory
     * - Full hash = filename
     *
     * Example: hash "abc123..." → stored at ".chunks/ab/abc123..."
     *
     * Why? To avoid having thousands of files in one directory.
     */
    public void storeChunk(String hash, byte[] data) throws IOException {
        if (hash == null || hash.length() < 2) {
            throw new IllegalArgumentException("Invalid hash");
        }

        // Verify the data matches the hash
        String actualHash = calculateHash(data);
        if (!actualHash.equals(hash)) {
            throw new IOException("Data hash doesn't match expected hash");
        }

        // Create subdirectory (first 2 chars of hash)
        String subdir = hash.substring(0, 2);
        Path subdirPath = storageRoot.resolve(subdir);
        Files.createDirectories(subdirPath);

        // Write chunk file
        Path chunkPath = subdirPath.resolve(hash);
        Files.write(chunkPath, data);

        logger.debug("Stored chunk: {} ({} bytes)", hash.substring(0, 8) + "...", data.length);
    }

    /**
     * Retrieves a chunk by its hash.
     *
     * @param hash The hash of the chunk to retrieve
     * @return The chunk data as bytes
     * @throws IOException If chunk doesn't exist or can't be read
     */
    public byte[] retrieveChunk(String hash) throws IOException {
        Path chunkPath = getChunkPath(hash);

        if (!Files.exists(chunkPath)) {
            throw new IOException("Chunk not found: " + hash);
        }

        byte[] data = Files.readAllBytes(chunkPath);

        // Verify integrity
        String actualHash = calculateHash(data);
        if (!actualHash.equals(hash)) {
            logger.error("Chunk integrity check failed: {}", hash);
            throw new IOException("Chunk corrupted: hash mismatch");
        }

        logger.debug("Retrieved chunk: {} ({} bytes)", hash.substring(0, 8) + "...", data.length);
        return data;
    }

    /**
     * Checks if a chunk with the given hash exists in storage.
     */
    public boolean hasChunk(String hash) {
        return Files.exists(getChunkPath(hash));
    }

    /**
     * Deletes a chunk from storage.
     */
    public void deleteChunk(String hash) throws IOException {
        Path chunkPath = getChunkPath(hash);
        if (Files.exists(chunkPath)) {
            Files.delete(chunkPath);
            logger.debug("Deleted chunk: {}", hash.substring(0, 8) + "...");
        }
    }

    /**
     * Gets the file path for a chunk with the given hash.
     */
    private Path getChunkPath(String hash) {
        if (hash == null || hash.length() < 2) {
            throw new IllegalArgumentException("Invalid hash");
        }
        String subdir = hash.substring(0, 2);
        return storageRoot.resolve(subdir).resolve(hash);
    }

    /**
     * Ensures the storage root directory exists.
     */
    private void ensureStorageExists() {
        try {
            Files.createDirectories(storageRoot);
            logger.debug("Storage root: {}", storageRoot.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to create storage directory", e);
            throw new RuntimeException("Failed to initialize storage", e);
        }
    }

    /**
     * Calculates SHA-256 hash of data.
     */
    private String calculateHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
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

    /**
     * Returns the storage root path.
     */
    public Path getStorageRoot() {
        return storageRoot;
    }
}
