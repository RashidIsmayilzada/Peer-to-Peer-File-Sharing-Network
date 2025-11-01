package com.p2p.protocol;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

// Base class for all P2P protocol messages
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = HelloMessage.class, name = "HELLO"),
    @JsonSubTypes.Type(value = ManifestRequestMessage.class, name = "MANIFEST_REQUEST"),
    @JsonSubTypes.Type(value = ManifestResponseMessage.class, name = "MANIFEST_RESPONSE"),
    @JsonSubTypes.Type(value = ChunkRequestMessage.class, name = "CHUNK_REQUEST"),
    @JsonSubTypes.Type(value = ChunkResponseMessage.class, name = "CHUNK_RESPONSE"),
    @JsonSubTypes.Type(value = PeerListRequestMessage.class, name = "PEER_LIST_REQUEST"),
    @JsonSubTypes.Type(value = PeerListResponseMessage.class, name = "PEER_LIST_RESPONSE")
})
public abstract class Message {
    // Returns the type of this message
    public abstract MessageType getType();
}
