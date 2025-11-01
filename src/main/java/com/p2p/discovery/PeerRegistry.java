package com.p2p.discovery;

import com.p2p.core.PeerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// Registry of known peers in the network
public class PeerRegistry {
    private static final Logger logger = LoggerFactory.getLogger(PeerRegistry.class);

    private final Map<String, PeerInfo> peers = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> fileProviders = new ConcurrentHashMap<>();

    // Adds or updates a peer in the registry
    public void addPeer(PeerInfo peerInfo) {
        PeerInfo existing = peers.put(peerInfo.getPeerId(), peerInfo);

        if (existing == null) {
            logger.info("New peer discovered: {} at {}:{}",
                    peerInfo.getPeerId(), peerInfo.getHost(), peerInfo.getPort());
        } else {
            logger.debug("Updated peer info: {}", peerInfo.getPeerId());
        }

        for (String fileId : peerInfo.getAvailableFiles()) {
            fileProviders.computeIfAbsent(fileId, k -> ConcurrentHashMap.newKeySet())
                    .add(peerInfo.getPeerId());
        }
    }

    // Removes a peer from the registry
    public void removePeer(String peerId) {
        PeerInfo removed = peers.remove(peerId);
        if (removed != null) {
            logger.info("Peer removed: {}", peerId);

            for (Set<String> providers : fileProviders.values()) {
                providers.remove(peerId);
            }
        }
    }

    // Gets information about a specific peer
    public PeerInfo getPeer(String peerId) {
        return peers.get(peerId);
    }

    // Returns all known peers
    public List<PeerInfo> getAllPeers() {
        return new ArrayList<>(peers.values());
    }

    // Returns peers that have a specific file
    public List<PeerInfo> getPeersWithFile(String fileId) {
        Set<String> providerIds = fileProviders.get(fileId);
        if (providerIds == null || providerIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<PeerInfo> providers = new ArrayList<>();
        for (String peerId : providerIds) {
            PeerInfo peer = peers.get(peerId);
            if (peer != null) {
                providers.add(peer);
            }
        }

        return providers;
    }

    // Returns the number of known peers
    public int getPeerCount() {
        return peers.size();
    }

    // Returns the number of peers that have a specific file
    public int getProviderCount(String fileId) {
        Set<String> providers = fileProviders.get(fileId);
        return providers == null ? 0 : providers.size();
    }

    // Clears all peers from the registry
    public void clear() {
        peers.clear();
        fileProviders.clear();
        logger.info("Peer registry cleared");
    }
}
