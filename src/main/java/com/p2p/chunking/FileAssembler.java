package com.p2p.chunking;

import com.p2p.core.Manifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// Assembles downloaded chunks back into the original file
public class FileAssembler {
    private static final Logger logger = LoggerFactory.getLogger(FileAssembler.class);

    // Assembles chunks into a file with verification
    public void assembleFile(Manifest manifest, byte[][] chunks, File outputFile) throws IOException {
        logger.info("Assembling file: {} from {} chunks", manifest.getFilename(), chunks.length);

        if (chunks.length != manifest.getChunkCount()) {
            throw new IllegalArgumentException(
                    String.format("Expected %d chunks but got %d", manifest.getChunkCount(), chunks.length)
            );
        }

        for (int i = 0; i < chunks.length; i++) {
            String expectedHash = manifest.getChunk(i).getHash();
            String actualHash = calculateHash(chunks[i]);

            if (!actualHash.equals(expectedHash)) {
                throw new IllegalArgumentException(
                        String.format("Chunk %d hash mismatch: expected %s but got %s",
                                i, expectedHash, actualHash)
                );
            }

            logger.debug("Chunk {} verified: hash matches", i);
        }

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            for (int i = 0; i < chunks.length; i++) {
                fos.write(chunks[i]);
                logger.debug("Wrote chunk {} ({} bytes)", i, chunks[i].length);
            }
        }

        logger.info("File assembled successfully: {} ({} bytes)",
                outputFile.getName(), outputFile.length());

        if (outputFile.length() != manifest.getFileSize()) {
            throw new IOException(
                    String.format("File size mismatch: expected %d but got %d",
                            manifest.getFileSize(), outputFile.length())
            );
        }
    }

    // Calculates SHA-256 hash of data
    private String calculateHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
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
}
