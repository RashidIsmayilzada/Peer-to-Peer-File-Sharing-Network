package com.p2p.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

// File metadata containing chunk information and verification hashes
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Manifest {
    private final String fileId;           // Unique file identifier (SHA-256)
    private final String filename;         // Original filename
    private final long fileSize;           // Total file size in bytes
    private final int chunkSize;           // Size of each chunk (last may be smaller)
    private final List<ChunkInfo> chunks;  // List of all chunks with metadata

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

    // Returns total number of chunks
    public int getChunkCount() {
        return chunks.size();
    }

    // Gets chunk metadata by index
    public ChunkInfo getChunk(int index) {
        if (index < 0 || index >= chunks.size()) {
            throw new IllegalArgumentException("Invalid chunk index: " + index);
        }
        return chunks.get(index);
    }
}
