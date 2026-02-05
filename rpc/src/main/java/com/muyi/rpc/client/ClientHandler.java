package com.muyi.rpc.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.muyi.rpc.core.RpcResponse;
import com.muyi.rpc.protocol.MessageType;
import com.muyi.rpc.protocol.RpcMessage;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * 客户端业务处理器
 *
 * @author muyi
 */
public class ClientHandler extends SimpleChannelInboundHandler<RpcMessage> {
    
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    
    private final RpcClient rpcClient;
    
    public ClientHandler(RpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) {
        // Pattern Matching for instanceof (JDK 16+)
        if (msg.getMessageType() == MessageType.RESPONSE && msg.getData() instanceof RpcResponse response) {
            rpcClient.handleResponse(response);
        }
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // 使用 debug 级别，因为客户端连接关闭在正常场景下也可能发生
        if (logger.isDebugEnabled()) {
            logger.debug("Connection closed: {}", ctx.channel().remoteAddress());
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Client handler error: {}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }
}
