package com.p2p.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.p2p.core.PeerInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

// Response containing list of known peers
@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PeerListResponseMessage extends Message {
    private final List<PeerInfo> peers;  // List of known peers

    @JsonCreator
    public PeerListResponseMessage(@JsonProperty("peers") List<PeerInfo> peers) {
        this.peers = peers;
    }

    @Override
    public MessageType getType() {
        return MessageType.PEER_LIST_RESPONSE;
    }
}
