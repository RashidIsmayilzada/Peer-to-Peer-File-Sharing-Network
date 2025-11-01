package com.p2p.network;

import com.p2p.core.PeerInfo;
import com.p2p.protocol.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

// Handles incoming messages from other peers
public class PeerServerHandler extends SimpleChannelInboundHandler<Message> {
    private static final Logger logger = LoggerFactory.getLogger(PeerServerHandler.class);
    private final PeerServer server;

    public PeerServerHandler(PeerServer server) {
        this.server = server;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        logger.info("New connection from: {}", ctx.channel().remoteAddress());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
        logger.debug("Received message: {} from {}", msg.getType(), ctx.channel().remoteAddress());

        switch (msg.getType()) {
            case HELLO:
                handleHello(ctx, (HelloMessage) msg);
                break;

            case MANIFEST_REQUEST:
                handleManifestRequest(ctx, (ManifestRequestMessage) msg);
                break;

            case CHUNK_REQUEST:
                handleChunkRequest(ctx, (ChunkRequestMessage) msg);
                break;

            case PEER_LIST_REQUEST:
                handlePeerListRequest(ctx, (PeerListRequestMessage) msg);
                break;

            case PEER_LIST_RESPONSE:
                handlePeerListResponse(ctx, (PeerListResponseMessage) msg);
                break;

            default:
                logger.warn("Unexpected message type: {}", msg.getType());
        }
    }

    private void handleHello(ChannelHandlerContext ctx, HelloMessage msg) {
        logger.info("Received HELLO from peer: {} with {} files",
                msg.getPeerId(), msg.getAvailableFiles().size());

        // Register the peer in our peer registry
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        String host = remoteAddress.getAddress().getHostAddress();

        PeerInfo peerInfo = new PeerInfo(
                msg.getPeerId(),
                host,
                msg.getPort(),
                msg.getAvailableFiles()
        );
        server.getPeerRegistry().addPeer(peerInfo);

        // Send back our own HELLO
        HelloMessage response = new HelloMessage(
                server.getPeerId(),
                server.getAvailableFiles(),
                server.getPort()
        );
        ctx.writeAndFlush(response);

        logger.info("Registered peer {} at {}:{}", msg.getPeerId(), host, msg.getPort());
    }

    private void handleManifestRequest(ChannelHandlerContext ctx, ManifestRequestMessage msg) {
        logger.info("Received MANIFEST_REQUEST for file: {}", msg.getFileId());

        try {
            // Look up manifest
            var manifest = server.getManifestStorage().retrieveManifest(msg.getFileId());

            if (manifest == null) {
                logger.warn("Manifest not found for file: {}", msg.getFileId());
                ctx.close();
                return;
            }

            // Send manifest response
            ManifestResponseMessage response = new ManifestResponseMessage(manifest);
            ctx.writeAndFlush(response);
            logger.info("Sent MANIFEST_RESPONSE for file: {}", manifest.getFilename());

        } catch (Exception e) {
            logger.error("Failed to handle manifest request", e);
            ctx.close();
        }
    }

    private void handleChunkRequest(ChannelHandlerContext ctx, ChunkRequestMessage msg) {
        logger.info("Received CHUNK_REQUEST for file: {}, chunk: {}",
                msg.getFileId(), msg.getChunkIndex());

        try {
            // Get manifest to find chunk hash
            var manifest = server.getManifestStorage().retrieveManifest(msg.getFileId());

            if (manifest == null) {
                logger.warn("Manifest not found for file: {}", msg.getFileId());
                ctx.close();
                return;
            }

            if (msg.getChunkIndex() < 0 || msg.getChunkIndex() >= manifest.getChunkCount()) {
                logger.warn("Invalid chunk index: {}", msg.getChunkIndex());
                ctx.close();
                return;
            }

            // Get chunk info
            var chunkInfo = manifest.getChunk(msg.getChunkIndex());

            // Load chunk data
            byte[] chunkData = server.getChunkStorage().retrieveChunk(chunkInfo.getHash());

            // Send chunk response
            ChunkResponseMessage response = new ChunkResponseMessage(
                    msg.getFileId(),
                    msg.getChunkIndex(),
                    chunkData,
                    chunkInfo.getHash()
            );

            ctx.writeAndFlush(response);
            logger.info("Sent CHUNK_RESPONSE for file: {}, chunk: {} ({} bytes)",
                    msg.getFileId(), msg.getChunkIndex(), chunkData.length);

        } catch (Exception e) {
            logger.error("Failed to handle chunk request", e);
            ctx.close();
        }
    }

    private void handlePeerListRequest(ChannelHandlerContext ctx, PeerListRequestMessage msg) {
        logger.info("Received PEER_LIST_REQUEST");

        // Send list of all known peers
        PeerListResponseMessage response = new PeerListResponseMessage(
                server.getPeerRegistry().getAllPeers()
        );
        ctx.writeAndFlush(response);

        logger.info("Sent PEER_LIST_RESPONSE with {} peers",
                server.getPeerRegistry().getPeerCount());
    }

    private void handlePeerListResponse(ChannelHandlerContext ctx, PeerListResponseMessage msg) {
        logger.info("Received PEER_LIST_RESPONSE with {} peers", msg.getPeers().size());

        // Add all received peers to our registry
        for (PeerInfo peer : msg.getPeers()) {
            // Don't add ourselves
            if (!peer.getPeerId().equals(server.getPeerId())) {
                server.getPeerRegistry().addPeer(peer);
            }
        }

        logger.info("Discovered {} new peers, total known peers: {}",
                msg.getPeers().size(), server.getPeerRegistry().getPeerCount());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Error in server handler", cause);
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.info("Connection closed: {}", ctx.channel().remoteAddress());
    }
}
