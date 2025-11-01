package com.p2p.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents information about a peer in the network.
 * A "peer" is another computer running this P2P software.
 *
 * Think of this as a contact card with:
 * - Name (peerId)
 * - Address (host)
 * - Phone number (port)
 * - Last time you talked (lastSeen)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PeerInfo {
    /**
     * Unique identifier for this peer (UUID).
     * Generated when the peer starts up.
     */
    private String peerId;

    /**
     * IP address or hostname of the peer (e.g., "192.168.1.100" or "localhost").
     */
    private String host;

    /**
     * Port number the peer is listening on (e.g., 6881).
     */
    private int port;

    /**
     * Timestamp of last successful communication (milliseconds since epoch).
     */
    private long lastSeen;

    /**
     * List of file IDs that this peer has available for sharing.
     */
    private List<String> availableFiles = new ArrayList<>();

    /**
     * Creates a new PeerInfo with current timestamp.
     */
    public PeerInfo(String host, int port) {
        this.peerId = UUID.randomUUID().toString();
        this.host = host;
        this.port = port;
        this.lastSeen = System.currentTimeMillis();
        this.availableFiles = new ArrayList<>();
    }

    /**
     * Creates a new PeerInfo with all fields.
     */
    public PeerInfo(String peerId, String host, int port, List<String> availableFiles) {
        this.peerId = peerId;
        this.host = host;
        this.port = port;
        this.lastSeen = System.currentTimeMillis();
        this.availableFiles = availableFiles != null ? new ArrayList<>(availableFiles) : new ArrayList<>();
    }

    /**
     * Updates the last seen timestamp to now.
     */
    public void updateLastSeen() {
        this.lastSeen = System.currentTimeMillis();
    }

    /**
     * Returns a string representation like "host:port".
     */
    public String getAddress() {
        return host + ":" + port;
    }
}
