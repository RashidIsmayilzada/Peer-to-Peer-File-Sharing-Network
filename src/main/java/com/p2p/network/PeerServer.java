package com.p2p.network;

import com.p2p.discovery.PeerRegistry;
import com.p2p.storage.ChunkStorage;
import com.p2p.storage.ManifestStorage;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Netty-based TCP server that listens for incoming peer connections
public class PeerServer {
    private static final Logger logger = LoggerFactory.getLogger(PeerServer.class);

    @Getter
    private final String peerId;

    @Getter
    private final int port;

    @Getter
    private final ManifestStorage manifestStorage;

    @Getter
    private final ChunkStorage chunkStorage;

    @Getter
    private final PeerRegistry peerRegistry;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    // Temporary storage for available file IDs (TODO: Replace with proper storage layer)
    private final List<String> availableFiles = new ArrayList<>();

    public PeerServer(int port) {
        this.peerId = UUID.randomUUID().toString();
        this.port = port;
        this.manifestStorage = new ManifestStorage();
        this.chunkStorage = new ChunkStorage();
        this.peerRegistry = new PeerRegistry();
    }

    public PeerServer(int port, ManifestStorage manifestStorage, ChunkStorage chunkStorage) {
        this.peerId = UUID.randomUUID().toString();
        this.port = port;
        this.manifestStorage = manifestStorage;
        this.chunkStorage = chunkStorage;
        this.peerRegistry = new PeerRegistry();
    }

    // Starts the server and begins listening for connections
    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            // Frame decoder with 100MB max frame size for large files
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(100 * 1024 * 1024, 0, 4, 0, 4));
                            pipeline.addLast(new MessageCodec());
                            pipeline.addLast(new PeerServerHandler(PeerServer.this));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();

            logger.info("Peer server started on port {} with ID: {}", port, peerId);

            serverChannel.closeFuture().sync();
        } finally {
            shutdown();
        }
    }

    // Shuts down the server gracefully
    public void shutdown() {
        logger.info("Shutting down peer server...");
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
    }

    // Returns list of file IDs this peer has available
    public List<String> getAvailableFiles() {
        return new ArrayList<>(availableFiles);
    }

    // Adds a file ID to the available files list
    public void addAvailableFile(String fileId) {
        availableFiles.add(fileId);
        logger.info("Added file to available list: {}", fileId);
    }
}
