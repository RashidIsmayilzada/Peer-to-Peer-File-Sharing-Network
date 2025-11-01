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

// Splits files into fixed-size chunks and generates hashes for each chunk
public class FileChunker {
    private static final Logger logger = LoggerFactory.getLogger(FileChunker.class);

    // Default chunk size: 256 KiB
    public static final int DEFAULT_CHUNK_SIZE = 256 * 1024;

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

    // Splits a file into chunks and returns metadata about each chunk
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
                String hash = calculateHash(buffer, bytesRead);
                ChunkInfo chunkInfo = new ChunkInfo(chunkIndex, hash, bytesRead);
                chunks.add(chunkInfo);

                logger.debug("Chunk {}: {} bytes, hash: {}", chunkIndex, bytesRead, hash.substring(0, 8) + "...");
                chunkIndex++;
            }
        }

        logger.info("File chunked into {} pieces", chunks.size());
        return chunks;
    }

    // Reads a specific chunk from a file by index
    public byte[] readChunk(File file, int chunkIndex) throws IOException {
        if (chunkIndex < 0) {
            throw new IllegalArgumentException("Chunk index must be non-negative");
        }

        long offset = (long) chunkIndex * chunkSize;

        if (offset >= file.length()) {
            throw new IllegalArgumentException("Chunk index out of bounds");
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            long skipped = fis.skip(offset);
            if (skipped != offset) {
                throw new IOException("Failed to skip to chunk position");
            }

            byte[] buffer = new byte[chunkSize];
            int bytesRead = fis.read(buffer);

            if (bytesRead == -1) {
                throw new IOException("Failed to read chunk");
            }

            byte[] chunk = new byte[bytesRead];
            System.arraycopy(buffer, 0, chunk, 0, bytesRead);
            return chunk;
        }
    }

    // Calculates SHA-256 hash of data
    private String calculateHash(byte[] data, int length) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(java.util.Arrays.copyOf(data, length));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    // Converts bytes to hexadecimal string
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // Calculates the number of chunks for a file of given size
    public int calculateChunkCount(long fileSize) {
        return (int) ((fileSize + chunkSize - 1) / chunkSize);
    }

    public int getChunkSize() {
        return chunkSize;
    }
}
