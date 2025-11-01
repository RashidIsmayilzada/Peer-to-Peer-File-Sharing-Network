package com.p2p.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Peer connection information (ID, host, port, available files)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PeerInfo {
    private String peerId;                      // Unique peer identifier (UUID)
    private String host;                        // IP address or hostname
    private int port;                           // Port number
    private long lastSeen;                      // Last communication timestamp
    private List<String> availableFiles = new ArrayList<>();  // File IDs this peer has

    // Creates peer with auto-generated ID
    public PeerInfo(String host, int port) {
        this.peerId = UUID.randomUUID().toString();
        this.host = host;
        this.port = port;
        this.lastSeen = System.currentTimeMillis();
        this.availableFiles = new ArrayList<>();
    }

    // Creates peer with specified files
    public PeerInfo(String peerId, String host, int port, List<String> availableFiles) {
        this.peerId = peerId;
        this.host = host;
        this.port = port;
        this.lastSeen = System.currentTimeMillis();
        this.availableFiles = availableFiles != null ? new ArrayList<>(availableFiles) : new ArrayList<>();
    }

    // Updates last seen timestamp to current time
    public void updateLastSeen() {
        this.lastSeen = System.currentTimeMillis();
    }

    // Returns address in "host:port" format
    public String getAddress() {
        return host + ":" + port;
    }
}
