package com.p2p.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Metadata for a single file chunk (index, hash, size)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChunkInfo {
    private int index;       // Chunk index in file (0-based)
    private String hash;     // SHA-256 hash for verification
    private long size;       // Chunk size in bytes
}
