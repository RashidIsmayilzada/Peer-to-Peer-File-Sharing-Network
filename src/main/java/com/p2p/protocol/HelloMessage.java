package com.p2p.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * HELLO message sent when two peers first connect.
 *
 * Example conversation:
 * Peer A: "Hello! I'm peer-123 and I have files [abc, def]"
 * Peer B: "Hello! I'm peer-456 and I have files [xyz]"
 */
@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HelloMessage extends Message {
    /**
     * The sender's peer ID.
     */
    private final String peerId;

    /**
     * List of file IDs this peer has available for sharing.
     */
    private final List<String> availableFiles;

    /**
     * Port this peer is listening on.
     */
    private final int port;

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
