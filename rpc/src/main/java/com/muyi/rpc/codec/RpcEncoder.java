package com.muyi.rpc.codec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.muyi.rpc.compress.Compressor;
import com.muyi.rpc.compress.CompressorFactory;
import com.muyi.rpc.protocol.RpcMessage;
import com.muyi.rpc.protocol.RpcProtocol;
import com.muyi.rpc.serialize.SerializerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * RPC消息编码器
 * 将RpcMessage对象编码为字节流
 * 
 * 协议格式（16字节头 + 数据）：
 * +--------+--------+--------+--------+--------+--------+--------+--------+
 * |  魔数  |序列化  | 压缩   | 消息类型 |         消息ID（8字节）           |
 * +--------+--------+--------+--------+--------+--------+--------+--------+
 * |            数据长度（4字节）              |         数据内容...          |
 * +--------+--------+--------+--------+--------+--------+--------+--------+
 *
 * @author muyi
 */
public class RpcEncoder extends MessageToByteEncoder<RpcMessage> {
    
    private static final Logger logger = LoggerFactory.getLogger(RpcEncoder.class);
    
    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage msg, ByteBuf out) {
        try {
            // 写入魔数
            out.writeByte(RpcProtocol.MAGIC);
            // 写入序列化类型
            out.writeByte(msg.getSerializeType());
            
            // 序列化消息体
            byte[] body = new byte[0];
            if (msg.getData() != null) {
                body = SerializerFactory.get(msg.getSerializeType()).serialize(msg.getData());
                
                // 检查序列化后的大小，防止 OOM
                if (body.length > RpcProtocol.MAX_FRAME_LENGTH) {
                    throw new IllegalArgumentException(
                            "Serialized data too large: " + body.length + " bytes, max allowed: " + RpcProtocol.MAX_FRAME_LENGTH);
                }
            }
            
            // 判断是否需要压缩
            byte compressType = Compressor.NONE;
            if (body.length > 0 && CompressorFactory.shouldCompress(body.length)) {
                byte[] compressed = CompressorFactory.get(CompressorFactory.getDefaultType()).compress(body);
                
                // 只有压缩后确实更小才使用压缩（某些数据压缩后反而更大）
                if (compressed.length < body.length) {
                    compressType = CompressorFactory.getDefaultType();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Compress: {} -> {} bytes (ratio: {}%)", 
                                body.length, compressed.length, compressed.length * 100 / body.length);
                    }
                    body = compressed;
                } else if (logger.isDebugEnabled()) {
                    logger.debug("Skip compress: {} -> {} bytes (no benefit)", body.length, compressed.length);
                }
            }
            
            // 写入压缩类型
            out.writeByte(compressType);
            // 写入消息类型
            out.writeByte(msg.getMessageType());
            // 写入消息ID
            out.writeLong(msg.getMessageId());
            // 写入消息体长度
            out.writeInt(body.length);
            
            // 写入消息体
            if (body.length > 0) {
                out.writeBytes(body);
            }
            
            if (logger.isDebugEnabled()) {
                logger.debug("Encode message: type={}, id={}, compress={}, bodyLength={}", 
                        msg.getMessageType(), msg.getMessageId(), compressType, body.length);
            }
        } catch (Exception e) {
            logger.error("Encode message error", e);
            throw e;
        }
    }
}
