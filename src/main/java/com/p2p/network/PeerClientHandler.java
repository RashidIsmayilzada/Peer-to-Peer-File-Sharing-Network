package com.p2p.network;

import com.p2p.protocol.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles responses when we connect to other peers as a client.
 *
 * Think of this as your personal assistant when you visit someone else's house:
 * - Introduces you (sends HELLO)
 * - Listens to responses
 * - Handles what you receive
 */
public class PeerClientHandler extends SimpleChannelInboundHandler<Message> {
    private static final Logger logger = LoggerFactory.getLogger(PeerClientHandler.class);
    private final PeerClient client;

    public PeerClientHandler(PeerClient client) {
        this.client = client;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        logger.info("Connected to peer: {}", ctx.channel().remoteAddress());

        // Send HELLO message when connection is established
        HelloMessage hello = new HelloMessage(
                client.getLocalPeerId(),
                client.getAvailableFiles(),
                client.getLocalPort()
        );
        ctx.writeAndFlush(hello);
        logger.debug("Sent HELLO message");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
        logger.debug("Received message: {} from {}", msg.getType(), ctx.channel().remoteAddress());

        switch (msg.getType()) {
            case HELLO:
                handleHello(ctx, (HelloMessage) msg);
                break;

            case MANIFEST_RESPONSE:
                handleManifestResponse(ctx, (ManifestResponseMessage) msg);
                break;

            case CHUNK_RESPONSE:
                handleChunkResponse(ctx, (ChunkResponseMessage) msg);
                break;

            default:
                logger.warn("Unexpected message type: {}", msg.getType());
        }
    }

    private void handleHello(ChannelHandlerContext ctx, HelloMessage msg) {
        logger.info("Received HELLO from peer: {} with {} files",
                msg.getPeerId(), msg.getAvailableFiles().size());
        // TODO: Store peer info
    }

    private void handleManifestResponse(ChannelHandlerContext ctx, ManifestResponseMessage msg) {
        logger.info("Received MANIFEST_RESPONSE for file: {}",
                msg.getManifest().getFilename());
        // TODO: Store manifest and begin downloading chunks
    }

    private void handleChunkResponse(ChannelHandlerContext ctx, ChunkResponseMessage msg) {
        logger.info("Received CHUNK_RESPONSE for file: {}, chunk: {}",
                msg.getFileId(), msg.getChunkIndex());
        // TODO: Verify hash and store chunk
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Error in client handler", cause);
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.info("Disconnected from peer: {}", ctx.channel().remoteAddress());
    }
}
