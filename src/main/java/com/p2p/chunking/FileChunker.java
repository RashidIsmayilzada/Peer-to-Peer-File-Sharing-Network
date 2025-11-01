package com.p2p.chunking;

import com.p2p.core.ChunkInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Splits files into fixed-size chunks and generates hashes for each chunk.
 *
 * Think of this like a paper shredder that:
 * - Cuts a document into equal-sized strips
 * - Numbers each strip (0, 1, 2, ...)
 * - Takes a fingerprint of each strip (SHA-256 hash)
 * - Remembers how big each strip is
 */
public class FileChunker {
    private static final Logger logger = LoggerFactory.getLogger(FileChunker.class);

    /**
     * Default chunk size: 256 KiB (262,144 bytes).
     * This is a good balance between:
     * - Not too small (overhead)
     * - Not too large (parallelism)
     */
    public static final int DEFAULT_CHUNK_SIZE = 256 * 1024; // 256 KiB

    private final int chunkSize;

    public FileChunker() {
        this(DEFAULT_CHUNK_SIZE);
    }

    public FileChunker(int chunkSize) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be positive");
        }
        this.chunkSize = chunkSize;
    }

    /**
     * Splits a file into chunks and returns metadata about each chunk.
     *
     * Example:
     * - File: 1 MB (1,048,576 bytes)
     * - Chunk size: 256 KB (262,144 bytes)
     * - Result: 4 chunks (0, 1, 2, 3)
     *
     * @param file The file to chunk
     * @return List of ChunkInfo with index, hash, and size for each chunk
     */
    public List<ChunkInfo> chunkFile(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("File not found: " + file.getAbsolutePath());
        }

        if (!file.isFile()) {
            throw new IOException("Not a file: " + file.getAbsolutePath());
        }

        logger.info("Chunking file: {} ({} bytes, chunk size: {} bytes)",
                file.getName(), file.length(), chunkSize);

        List<ChunkInfo> chunks = new ArrayList<>();
        byte[] buffer = new byte[chunkSize];
        int chunkIndex = 0;

        try (FileInputStream fis = new FileInputStream(file)) {
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                // Calculate hash of this chunk
                String hash = calculateHash(buffer, bytesRead);

                // Create chunk info
                ChunkInfo chunkInfo = new ChunkInfo(chunkIndex, hash, bytesRead);
                chunks.add(chunkInfo);

                logger.debug("Chunk {}: {} bytes, hash: {}", chunkIndex, bytesRead, hash.substring(0, 8) + "...");
                chunkIndex++;
            }
        }

        logger.info("File chunked into {} pieces", chunks.size());
        return chunks;
    }

    /**
     * Reads a specific chunk from a file.
     *
     * @param file The file to read from
     * @param chunkIndex The index of the chunk to read (0-based)
     * @return The chunk data as bytes
     */
    public byte[] readChunk(File file, int chunkIndex) throws IOException {
        if (chunkIndex < 0) {
            throw new IllegalArgumentException("Chunk index must be non-negative");
        }

        long offset = (long) chunkIndex * chunkSize;

        if (offset >= file.length()) {
            throw new IllegalArgumentException("Chunk index out of bounds");
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            // Skip to the chunk position
            long skipped = fis.skip(offset);
            if (skipped != offset) {
                throw new IOException("Failed to skip to chunk position");
            }

            // Read the chunk
            byte[] buffer = new byte[chunkSize];
            int bytesRead = fis.read(buffer);

            if (bytesRead == -1) {
                throw new IOException("Failed to read chunk");
            }

            // Return exact size (last chunk might be smaller)
            byte[] chunk = new byte[bytesRead];
            System.arraycopy(buffer, 0, chunk, 0, bytesRead);
            return chunk;
        }
    }

    /**
     * Calculates SHA-256 hash of data.
     *
     * @param data The data to hash
     * @param length The number of bytes to hash (may be less than data.length)
     * @return Hex string representation of the hash
     */
    private String calculateHash(byte[] data, int length) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(java.util.Arrays.copyOf(data, length));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Converts bytes to hexadecimal string.
     *
     * Example: [0x1A, 0x2B] → "1a2b"
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Calculates the number of chunks for a file of given size.
     */
    public int calculateChunkCount(long fileSize) {
        return (int) ((fileSize + chunkSize - 1) / chunkSize);
    }

    public int getChunkSize() {
        return chunkSize;
    }
}
