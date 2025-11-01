package com.p2p.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Message to request list of known peers from another peer.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PeerListRequestMessage extends Message {

    public PeerListRequestMessage(int ignored) {
        // Constructor for Jackson, no fields needed
    }

    @Override
    public MessageType getType() {
        return MessageType.PEER_LIST_REQUEST;
    }
}
