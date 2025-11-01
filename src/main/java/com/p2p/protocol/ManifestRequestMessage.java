package com.p2p.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request for a file's manifest (metadata).
 *
 * Example:
 * "Hey, can you send me the manifest for file abc123?"
 */
@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManifestRequestMessage extends Message {
    /**
     * The file ID we want the manifest for.
     */
    private final String fileId;

    @JsonCreator
    public ManifestRequestMessage(@JsonProperty("fileId") String fileId) {
        this.fileId = fileId;
    }

    @Override
    public MessageType getType() {
        return MessageType.MANIFEST_REQUEST;
    }
}
