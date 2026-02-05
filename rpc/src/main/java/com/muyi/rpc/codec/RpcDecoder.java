package com.muyi.rpc.codec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.muyi.rpc.compress.Compressor;
import com.muyi.rpc.compress.CompressorFactory;
import com.muyi.rpc.core.RpcRequest;
import com.muyi.rpc.core.RpcResponse;
import com.muyi.rpc.protocol.MessageType;
import com.muyi.rpc.protocol.RpcMessage;
import com.muyi.rpc.protocol.RpcProtocol;
import com.muyi.rpc.serialize.SerializerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * RPC消息解码器
 * 将字节流解码为RpcMessage对象
 * 继承LengthFieldBasedFrameDecoder自动处理粘包/拆包问题
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
public class RpcDecoder extends LengthFieldBasedFrameDecoder {
    
    private static final Logger logger = LoggerFactory.getLogger(RpcDecoder.class);
    
    public RpcDecoder() {
        super(
            RpcProtocol.MAX_FRAME_LENGTH,  // 最大帧长度
            12,                             // 长度字段偏移量（魔数1+序列化1+压缩1+消息类型1+消息ID8=12）
            4,                              // 长度字段长度
            0,                              // 长度调整
            0                               // 需要跳过的字节数
        );
    }
    
    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        // 先调用父类解码，处理粘包/拆包
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }
        
        try {
            return decodeFrame(frame);
        } finally {
            frame.release();
        }
    }
    
    private RpcMessage decodeFrame(ByteBuf frame) {
        // 检查魔数
        byte magic = frame.readByte();
        if (magic != RpcProtocol.MAGIC) {
            throw new IllegalArgumentException("Invalid magic number: " + magic);
        }
        
        // 读取序列化类型
        byte serializeType = frame.readByte();
        
        // 读取压缩类型
        byte compressType = frame.readByte();
        
        // 读取消息类型
        byte messageType = frame.readByte();
        
        // 读取消息ID
        long messageId = frame.readLong();
        
        // 读取消息体长度
        int bodyLength = frame.readInt();
        
        // 创建消息对象
        RpcMessage message = new RpcMessage();
        message.setMessageType(messageType);
        message.setSerializeType(serializeType);
        message.setCompressType(compressType);
        message.setMessageId(messageId);
        
        // 读取并反序列化消息体
        if (bodyLength > 0) {
            byte[] body = new byte[bodyLength];
            frame.readBytes(body);
            
            // 解压缩
            if (compressType != Compressor.NONE) {
                body = CompressorFactory.get(compressType).decompress(body);
                // 校验解压后大小，防止压缩炸弹攻击
                if (body.length > RpcProtocol.MAX_FRAME_LENGTH) {
                    throw new IllegalArgumentException(
                            "Decompressed data too large: " + body.length + " bytes, max allowed: " + RpcProtocol.MAX_FRAME_LENGTH);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Decompress: {} -> {} bytes", bodyLength, body.length);
                }
            }
            
            // 根据消息类型反序列化
            Class<?> clazz = getMessageClass(messageType);
            if (clazz != null) {
                Object data = SerializerFactory.get(serializeType).deserialize(body, clazz);
                message.setData(data);
            }
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug("Decode message: type={}, id={}, compress={}, bodyLength={}", 
                    messageType, messageId, compressType, bodyLength);
        }
        
        return message;
    }
    
    /**
     * 根据消息类型获取对应的类
     */
    private Class<?> getMessageClass(byte messageType) {
        return switch (messageType) {
            case MessageType.REQUEST -> RpcRequest.class;
            case MessageType.RESPONSE -> RpcResponse.class;
            case MessageType.HEARTBEAT_REQUEST, MessageType.HEARTBEAT_RESPONSE ->
                    null; // 心跳消息无消息体
            default -> throw new IllegalArgumentException("Unknown message type: " + messageType);
        };
    }
}
