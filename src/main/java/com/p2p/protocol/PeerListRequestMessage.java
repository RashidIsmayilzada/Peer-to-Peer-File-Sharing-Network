package com.p2p.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

// Request for list of known peers from another peer
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PeerListRequestMessage extends Message {

    @Override
    public MessageType getType() {
        return MessageType.PEER_LIST_REQUEST;
    }
}
