package com.p2p.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

// Netty-based TCP client for connecting to other peers
public class PeerClient {
    private static final Logger logger = LoggerFactory.getLogger(PeerClient.class);

    @Getter
    private final String localPeerId;

    @Getter
    private final int localPort;

    private EventLoopGroup workerGroup;
    private Channel channel;

    // Temporary storage for available file IDs
    private final List<String> availableFiles = new ArrayList<>();

    public PeerClient(String localPeerId, int localPort) {
        this.localPeerId = localPeerId;
        this.localPort = localPort;
    }

    // Connects to a remote peer at the specified host and port
    public void connect(String host, int port) throws InterruptedException {
        workerGroup = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new MessageCodec());
                            pipeline.addLast(new PeerClientHandler(PeerClient.this));
                        }
                    })
                    .option(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture future = bootstrap.connect(host, port).sync();
            channel = future.channel();

            logger.info("Connected to peer at {}:{}", host, port);

            channel.closeFuture().sync();
        } finally {
            disconnect();
        }
    }

    // Disconnects from the peer and cleans up resources
    public void disconnect() {
        logger.info("Disconnecting from peer...");
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }

    // Returns list of file IDs this peer has available
    public List<String> getAvailableFiles() {
        return new ArrayList<>(availableFiles);
    }

    // Sends a message to the connected peer
    public void sendMessage(Object message) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(message);
            logger.debug("Sent message: {}", message.getClass().getSimpleName());
        } else {
            logger.warn("Cannot send message - channel not active");
        }
    }
}
