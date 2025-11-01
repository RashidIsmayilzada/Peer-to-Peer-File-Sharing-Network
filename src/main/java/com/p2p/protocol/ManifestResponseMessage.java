package com.p2p.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.p2p.core.Manifest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Response containing a file's manifest.
 *
 * Example:
 * "Sure! Here's the manifest for file abc123:
 *  - Filename: movie.mp4
 *  - Size: 10MB
 *  - 40 chunks of 256KB each
 *  - Here are the hashes for verification..."
 */
@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManifestResponseMessage extends Message {
    /**
     * The manifest containing all file metadata and chunk information.
     */
    private final Manifest manifest;

    @JsonCreator
    public ManifestResponseMessage(@JsonProperty("manifest") Manifest manifest) {
        this.manifest = manifest;
    }

    @Override
    public MessageType getType() {
        return MessageType.MANIFEST_RESPONSE;
    }
}
