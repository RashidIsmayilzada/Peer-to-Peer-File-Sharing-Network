package com.p2p.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Base64;

// Response containing chunk data (Base64 encoded)
@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChunkResponseMessage extends Message {
    private final String fileId;      // File this chunk belongs to
    private final int chunkIndex;     // Chunk index
    private final String data;        // Base64-encoded chunk data
    private final String hash;        // SHA-256 hash for verification

    @JsonCreator
    public ChunkResponseMessage(
            @JsonProperty("fileId") String fileId,
            @JsonProperty("chunkIndex") int chunkIndex,
            @JsonProperty("data") String data,
            @JsonProperty("hash") String hash) {
        this.fileId = fileId;
        this.chunkIndex = chunkIndex;
        this.data = data;
        this.hash = hash;
    }

    // Constructor accepting raw bytes (encodes to Base64)
    public ChunkResponseMessage(String fileId, int chunkIndex, byte[] rawData, String hash) {
        this.fileId = fileId;
        this.chunkIndex = chunkIndex;
        this.data = Base64.getEncoder().encodeToString(rawData);
        this.hash = hash;
    }

    // Decodes Base64 data back to raw bytes
    public byte[] getDataBytes() {
        return Base64.getDecoder().decode(data);
    }

    @Override
    public MessageType getType() {
        return MessageType.CHUNK_RESPONSE;
    }
}
