package com.p2p.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Manifest contains metadata about a shared file.
 * This is like a "table of contents" that describes:
 * - What the file is (name, size)
 * - How it's broken into chunks
 * - The hash of each chunk (for verification)
 *
 * Think of it like a recipe card that tells you:
 * - What dish you're making (filename)
 * - All the ingredients you need (chunks)
 * - How to verify each ingredient is correct (hashes)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Manifest {
    /**
     * Unique identifier for this file (SHA-256 of the manifest itself).
     * This is like a fingerprint for the file.
     */
    private final String fileId;

    /**
     * Original filename (e.g., "movie.mp4", "document.pdf").
     */
    private final String filename;

    /**
     * Total size of the file in bytes.
     */
    private final long fileSize;

    /**
     * Size of each chunk in bytes (typically 256 KiB = 262144 bytes).
     * The last chunk may be smaller.
     */
    private final int chunkSize;

    /**
     * List of all chunks with their metadata (index, hash, size).
     */
    private final List<ChunkInfo> chunks;

    @JsonCreator
    public Manifest(
            @JsonProperty("fileId") String fileId,
            @JsonProperty("filename") String filename,
            @JsonProperty("fileSize") long fileSize,
            @JsonProperty("chunkSize") int chunkSize,
            @JsonProperty("chunks") List<ChunkInfo> chunks) {
        this.fileId = fileId;
        this.filename = filename;
        this.fileSize = fileSize;
        this.chunkSize = chunkSize;
        this.chunks = chunks;
    }

    /**
     * Returns the total number of chunks in this file.
     */
    public int getChunkCount() {
        return chunks.size();
    }

    /**
     * Gets information about a specific chunk by index.
     */
    public ChunkInfo getChunk(int index) {
        if (index < 0 || index >= chunks.size()) {
            throw new IllegalArgumentException("Invalid chunk index: " + index);
        }
        return chunks.get(index);
    }
}
