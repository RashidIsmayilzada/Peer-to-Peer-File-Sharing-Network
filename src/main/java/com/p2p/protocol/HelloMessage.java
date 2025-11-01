package com.p2p.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

// HELLO message for peer handshake (contains peer ID, available files, port)
@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HelloMessage extends Message {
    private final String peerId;           // Sender's peer ID
    private final List<String> availableFiles;  // File IDs available for sharing
    private final int port;                // Listening port

    @JsonCreator
    public HelloMessage(
            @JsonProperty("peerId") String peerId,
            @JsonProperty("availableFiles") List<String> availableFiles,
            @JsonProperty("port") int port) {
        this.peerId = peerId;
        this.availableFiles = availableFiles;
        this.port = port;
    }

    @Override
    public MessageType getType() {
        return MessageType.HELLO;
    }
}
