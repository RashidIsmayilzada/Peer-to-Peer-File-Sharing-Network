package com.p2p.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

// Request for a specific chunk of a file
@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChunkRequestMessage extends Message {
    private final String fileId;      // File containing the chunk
    private final int chunkIndex;     // Chunk index to retrieve

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
