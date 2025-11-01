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

// Codec for encoding/decoding messages to/from JSON over the network
public class MessageCodec extends MessageToMessageCodec<ByteBuf, Message> {
    private static final Logger logger = LoggerFactory.getLogger(MessageCodec.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Encodes a Message object to JSON bytes with a 4-byte length prefix
    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, List<Object> out) throws Exception {
        try {
            String json = objectMapper.writeValueAsString(msg);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

            ByteBuf buffer = ctx.alloc().buffer(4 + bytes.length);
            buffer.writeInt(bytes.length);
            buffer.writeBytes(bytes);
            out.add(buffer);

            logger.debug("Encoded message: {} ({} bytes)", msg.getType(), bytes.length);
        } catch (Exception e) {
            logger.error("Failed to encode message", e);
            throw e;
        }
    }

    // Decodes JSON bytes to a Message object (length prefix already removed by frame decoder)
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        try {
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
