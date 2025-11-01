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

// Generates manifests for files
public class ManifestGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ManifestGenerator.class);

    private final FileChunker chunker;

    public ManifestGenerator() {
        this.chunker = new FileChunker();
    }

    public ManifestGenerator(FileChunker chunker) {
        this.chunker = chunker;
    }

    // Generates a manifest for a file
    public Manifest generateManifest(File file) throws IOException {
        logger.info("Generating manifest for file: {}", file.getName());

        List<ChunkInfo> chunks = chunker.chunkFile(file);
        String fileId = generateFileId(file, chunks);

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

    // Generates a unique file ID based on file metadata and chunk hashes
    private String generateFileId(File file, List<ChunkInfo> chunks) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            digest.update(file.getName().getBytes());
            digest.update(longToBytes(file.length()));

            for (ChunkInfo chunk : chunks) {
                digest.update(chunk.getHash().getBytes());
            }

            byte[] hash = digest.digest();
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    // Converts a long to bytes
    private byte[] longToBytes(long value) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return result;
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
