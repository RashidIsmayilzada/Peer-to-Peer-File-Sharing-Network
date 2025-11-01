package com.p2p.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p2p.protocol.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Codec for encoding/decoding messages to/from JSON over the network.
 *
 * Think of this as a translator:
 * - Outgoing: Converts Java objects → JSON → bytes with length prefix
 * - Incoming: Converts bytes → JSON → Java objects (after LengthFieldBasedFrameDecoder)
 */
public class MessageCodec extends MessageToMessageCodec<ByteBuf, Message> {
    private static final Logger logger = LoggerFactory.getLogger(MessageCodec.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Encodes a Message object to JSON bytes with a 4-byte length prefix.
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, List<Object> out) throws Exception {
        try {
            String json = objectMapper.writeValueAsString(msg);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

            // Create a buffer with length prefix (4 bytes) + JSON data
            ByteBuf buffer = ctx.alloc().buffer(4 + bytes.length);
            buffer.writeInt(bytes.length);  // Length prefix
            buffer.writeBytes(bytes);       // JSON data
            out.add(buffer);

            logger.debug("Encoded message: {} ({} bytes)", msg.getType(), bytes.length);
        } catch (Exception e) {
            logger.error("Failed to encode message", e);
            throw e;
        }
    }

    /**
     * Decodes JSON bytes to a Message object.
     * Note: LengthFieldBasedFrameDecoder strips the length prefix before this is called.
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        try {
            // Read all available bytes (length prefix already removed by frame decoder)
            byte[] bytes = new byte[msg.readableBytes()];
            msg.readBytes(bytes);
            String json = new String(bytes, StandardCharsets.UTF_8);

            Message message = objectMapper.readValue(json, Message.class);
            out.add(message);

            logger.debug("Decoded message: {}", message.getType());
        } catch (Exception e) {
            logger.error("Failed to decode message", e);
            throw e;
        }
    }
}
