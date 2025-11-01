package com.p2p.protocol;

// Protocol message types for P2P communication
public enum MessageType {
    HELLO,                // Initial peer handshake
    MANIFEST_REQUEST,     // Request file manifest
    MANIFEST_RESPONSE,    // Send file manifest
    CHUNK_REQUEST,        // Request file chunk
    CHUNK_RESPONSE,       // Send file chunk
    PEER_LIST_REQUEST,    // Request known peers
    PEER_LIST_RESPONSE    // Send known peers
}
