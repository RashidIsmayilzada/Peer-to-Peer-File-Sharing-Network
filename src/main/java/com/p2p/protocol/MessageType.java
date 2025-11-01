package com.p2p.protocol;

/**
 * Enum representing different types of messages in the P2P protocol.
 */
public enum MessageType {
    /**
     * Initial handshake message when peers connect.
     */
    HELLO,

    /**
     * Request for a file's manifest (metadata).
     */
    MANIFEST_REQUEST,

    /**
     * Response containing a file's manifest.
     */
    MANIFEST_RESPONSE,

    /**
     * Request for a specific chunk of a file.
     */
    CHUNK_REQUEST,

    /**
     * Response containing chunk data.
     */
    CHUNK_RESPONSE,

    /**
     * Request for list of known peers.
     */
    PEER_LIST_REQUEST,

    /**
     * Response containing list of known peers.
     */
    PEER_LIST_RESPONSE
}
