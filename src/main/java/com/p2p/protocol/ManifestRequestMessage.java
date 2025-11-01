package com.p2p.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

// Request for a file's manifest by file ID
@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManifestRequestMessage extends Message {
    private final String fileId;  // Requested file ID

    @JsonCreator
    public ManifestRequestMessage(@JsonProperty("fileId") String fileId) {
        this.fileId = fileId;
    }

    @Override
    public MessageType getType() {
        return MessageType.MANIFEST_REQUEST;
    }
}
