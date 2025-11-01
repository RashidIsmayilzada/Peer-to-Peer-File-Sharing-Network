package com.p2p.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Base64;

/**
 * Response containing chunk data.
 *
 * Example:
 * "Here's chunk #5 of file abc123: [binary data...]"
 */
@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChunkResponseMessage extends Message {
    /**
     * The file ID this chunk belongs to.
     */
    private final String fileId;

    /**
     * The index of this chunk.
     */
    private final int chunkIndex;

    /**
     * The actual chunk data, Base64-encoded for JSON transport.
     */
    private final String data;

    /**
     * SHA-256 hash of the chunk (for verification).
     */
    private final String hash;

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

    /**
     * Constructor that takes raw bytes and encodes them.
     */
    public ChunkResponseMessage(String fileId, int chunkIndex, byte[] rawData, String hash) {
        this.fileId = fileId;
        this.chunkIndex = chunkIndex;
        this.data = Base64.getEncoder().encodeToString(rawData);
        this.hash = hash;
    }

    /**
     * Decodes the Base64 data back to raw bytes.
     */
    public byte[] getDataBytes() {
        return Base64.getDecoder().decode(data);
    }

    @Override
    public MessageType getType() {
        return MessageType.CHUNK_RESPONSE;
    }
}
