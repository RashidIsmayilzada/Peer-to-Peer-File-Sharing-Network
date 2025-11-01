package com.p2p.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request for a specific chunk of a file.
 *
 * Example:
 * "Please send me chunk #5 of file abc123"
 */
@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChunkRequestMessage extends Message {
    /**
     * The file ID containing the chunk.
     */
    private final String fileId;

    /**
     * The index of the chunk we want (0-based).
     */
    private final int chunkIndex;

    @JsonCreator
    public ChunkRequestMessage(
            @JsonProperty("fileId") String fileId,
            @JsonProperty("chunkIndex") int chunkIndex) {
        this.fileId = fileId;
        this.chunkIndex = chunkIndex;
    }

    @Override
    public MessageType getType() {
        return MessageType.CHUNK_REQUEST;
    }
}
