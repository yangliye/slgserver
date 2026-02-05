package com.muyi.rpc.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.muyi.rpc.protocol.MessageType;
import com.muyi.rpc.protocol.RpcMessage;
import com.muyi.rpc.serialize.SerializerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * 服务端心跳处理器
 *
 * @author muyi
 */
public class ServerHeartbeatHandler extends ChannelInboundHandlerAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerHeartbeatHandler.class);
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // Pattern Matching for instanceof (JDK 16+)
        if (msg instanceof RpcMessage message) {
            // 处理心跳请求
            if (message.getMessageType() == MessageType.HEARTBEAT_REQUEST) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Received heartbeat from: {}", ctx.channel().remoteAddress());
                }
                
                // 发送心跳响应
                RpcMessage response = new RpcMessage(
                        MessageType.HEARTBEAT_RESPONSE,
                        SerializerFactory.getDefaultType(),
                        message.getMessageId(),
                        null
                );
                ctx.writeAndFlush(response);
                return;
            }
        }
        
        // 传递给下一个处理器
        ctx.fireChannelRead(msg);
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // Pattern Matching for instanceof (JDK 16+)
        if (evt instanceof IdleStateEvent event && event.state() == IdleState.READER_IDLE) {
            // 读超时，客户端可能已断开连接
            logger.warn("Reader idle timeout, closing channel: {}", ctx.channel().remoteAddress());
            ctx.close();
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
