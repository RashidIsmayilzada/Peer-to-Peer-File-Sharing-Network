package com.p2p.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents information about a single file chunk.
 * A chunk is a small piece of a larger file (typically 256-512 KiB).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChunkInfo {
    /**
     * The index of this chunk in the file (0-based).
     */
    private int index;

    /**
     * SHA-256 hash of the chunk data (hex string).
     * Used to verify chunk integrity after transfer.
     */
    private String hash;

    /**
     * Size of this chunk in bytes.
     * Most chunks are the same size, but the last chunk may be smaller.
     */
    private long size;
}
