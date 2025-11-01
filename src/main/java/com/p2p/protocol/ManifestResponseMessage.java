package com.p2p.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.p2p.core.Manifest;
import lombok.Data;
import lombok.EqualsAndHashCode;

// Response containing file manifest with metadata and chunk information
@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManifestResponseMessage extends Message {
    private final Manifest manifest;  // File manifest with chunk details

    @JsonCreator
    public ManifestResponseMessage(@JsonProperty("manifest") Manifest manifest) {
        this.manifest = manifest;
    }

    @Override
    public MessageType getType() {
        return MessageType.MANIFEST_RESPONSE;
    }
}
