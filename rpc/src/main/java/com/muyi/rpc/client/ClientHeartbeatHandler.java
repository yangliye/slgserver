package com.muyi.rpc.client;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
 * 客户端心跳处理器
 *
 * @author muyi
 */
public class ClientHeartbeatHandler extends ChannelInboundHandlerAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(ClientHeartbeatHandler.class);
    
    private static final AtomicLong HEARTBEAT_ID = new AtomicLong(0);
    
    /** 心跳失败计数（使用原子类保证线程安全） */
    private final AtomicInteger heartbeatFailCount = new AtomicInteger(0);
    
    /** 最大心跳失败次数 */
    private static final int MAX_HEARTBEAT_FAIL = 3;
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // Pattern Matching for instanceof (JDK 16+)
        if (msg instanceof RpcMessage message) {
            if (message.getMessageType() == MessageType.HEARTBEAT_RESPONSE) {
                heartbeatFailCount.set(0);
                if (logger.isDebugEnabled()) {
                    logger.debug("Received heartbeat response from: {}", ctx.channel().remoteAddress());
                }
                return;
            }
        }
        
        ctx.fireChannelRead(msg);
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // Pattern Matching for instanceof (JDK 16+)
        if (evt instanceof IdleStateEvent event && event.state() == IdleState.WRITER_IDLE) {
            // 发送心跳
            int failCount = heartbeatFailCount.get();
            if (failCount >= MAX_HEARTBEAT_FAIL) {
                logger.warn("Heartbeat failed {} times, closing channel: {}", 
                        failCount, ctx.channel().remoteAddress());
                ctx.close();
                return;
            }
            
            // 发送心跳前先增加失败计数（预期会失败）
            // 收到响应时会重置为 0，所以只有真正超时没收到响应才会累计
            heartbeatFailCount.incrementAndGet();
            
            RpcMessage heartbeat = new RpcMessage(
                    MessageType.HEARTBEAT_REQUEST,
                    SerializerFactory.getDefaultType(),
                    HEARTBEAT_ID.incrementAndGet(),
                    null
            );
            
            ctx.writeAndFlush(heartbeat).addListener(future -> {
                if (!future.isSuccess()) {
                    // 发送失败，计数已增加，不需要再增加
                    logger.warn("Failed to send heartbeat to: {}", ctx.channel().remoteAddress());
                }
            });
            
            if (logger.isDebugEnabled()) {
                logger.debug("Send heartbeat to: {}", ctx.channel().remoteAddress());
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
