package com.p2p.network;

import com.p2p.chunking.FileAssembler;
import com.p2p.core.Manifest;
import com.p2p.protocol.*;
import com.p2p.storage.ChunkStorage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Manages downloading a file from a peer.
 *
 * This orchestrates the entire download process:
 * 1. Connect to peer
 * 2. Request manifest
 * 3. Download all chunks
 * 4. Verify and assemble file
 */
public class DownloadManager {
    private static final Logger logger = LoggerFactory.getLogger(DownloadManager.class);
    private static final int TIMEOUT_SECONDS = 120; // 2 minutes per chunk (for large files or slow networks)

    private final String localPeerId;
    private final int localPort;
    private final ChunkStorage chunkStorage;

    // Queues for receiving responses
    private final BlockingQueue<Manifest> manifestQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<ChunkResponseMessage> chunkQueue = new LinkedBlockingQueue<>();

    public DownloadManager(String localPeerId, int localPort, ChunkStorage chunkStorage) {
        this.localPeerId = localPeerId;
        this.localPort = localPort;
        this.chunkStorage = chunkStorage;
    }

    /**
     * Downloads a file from a peer.
     *
     * @param fileId The file ID to download
     * @param peerHost The peer's hostname
     * @param peerPort The peer's port
     * @param outputFile Where to save the downloaded file
     * @return The manifest of the downloaded file (contains original filename)
     */
    public Manifest downloadFile(String fileId, String peerHost, int peerPort, File outputFile) throws Exception {
        logger.info("Starting download of file: {} from {}:{}", fileId, peerHost, peerPort);

        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            // Add frame decoder to handle length-prefixed messages
                            // Parameters: maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip
                            // Max frame size: 100MB (to support large files and manifests)
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(100 * 1024 * 1024, 0, 4, 0, 4));
                            pipeline.addLast(new MessageCodec());
                            pipeline.addLast(new DownloadHandler());
                        }
                    })
                    .option(ChannelOption.SO_KEEPALIVE, true);

            // Connect to peer
            ChannelFuture future = bootstrap.connect(peerHost, peerPort).sync();
            Channel channel = future.channel();

            logger.info("Connected to peer at {}:{}", peerHost, peerPort);

            // Send HELLO
            HelloMessage hello = new HelloMessage(localPeerId, java.util.Collections.emptyList(), localPort);
            channel.writeAndFlush(hello).sync();

            // Request manifest
            logger.info("Requesting manifest for file: {}", fileId);
            ManifestRequestMessage manifestRequest = new ManifestRequestMessage(fileId);
            channel.writeAndFlush(manifestRequest).sync();

            // Wait for manifest response
            Manifest manifest = manifestQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (manifest == null) {
                throw new RuntimeException("Timeout waiting for manifest");
            }

            logger.info("Received manifest: {} ({} chunks)", manifest.getFilename(), manifest.getChunkCount());

            // Download chunks and write directly to file (streaming approach)
            // This avoids loading all chunks into memory at once
            logger.info("Starting streaming download to: {}", outputFile.getAbsolutePath());

            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(outputFile)) {
                for (int i = 0; i < manifest.getChunkCount(); i++) {
                    logger.info("Requesting chunk {}/{}", i + 1, manifest.getChunkCount());

                    ChunkRequestMessage chunkRequest = new ChunkRequestMessage(fileId, i);
                    channel.writeAndFlush(chunkRequest).sync();

                    // Wait for chunk response
                    ChunkResponseMessage chunkResponse = chunkQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    if (chunkResponse == null) {
                        throw new RuntimeException("Timeout waiting for chunk " + i);
                    }

                    byte[] chunkData = chunkResponse.getDataBytes();

                    // Verify chunk hash
                    String expectedHash = manifest.getChunk(i).getHash();
                    if (!chunkResponse.getHash().equals(expectedHash)) {
                        throw new RuntimeException("Chunk " + i + " hash mismatch!");
                    }

                    // Store chunk for future sharing
                    chunkStorage.storeChunk(chunkResponse.getHash(), chunkData);

                    // Write chunk directly to output file (streaming)
                    fos.write(chunkData);
                    fos.flush();

                    logger.info("Received and wrote chunk {}/{} ({} bytes)",
                            i + 1, manifest.getChunkCount(), chunkData.length);
                }
            }

            // Close connection
            channel.close().sync();

            logger.info("Download and assembly complete: {}", outputFile.getName());

            logger.info("Download complete: {}", outputFile.getName());

            return manifest;

        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    /**
     * Handler for download responses.
     */
    private class DownloadHandler extends SimpleChannelInboundHandler<Message> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
            logger.debug("Received message: {}", msg.getType());

            try {
                switch (msg.getType()) {
                    case HELLO:
                        // Ignore HELLO responses
                        logger.debug("Received HELLO response");
                        break;

                    case MANIFEST_RESPONSE:
                        ManifestResponseMessage manifestResponse = (ManifestResponseMessage) msg;
                        manifestQueue.offer(manifestResponse.getManifest());
                        break;

                    case CHUNK_RESPONSE:
                        ChunkResponseMessage chunkResponse = (ChunkResponseMessage) msg;
                        chunkQueue.offer(chunkResponse);
                        break;

                    default:
                        logger.warn("Unexpected message type: {}", msg.getType());
                }
            } catch (Exception e) {
                logger.error("Error handling message", e);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("Error in download handler", cause);
            ctx.close();
        }
    }
}
