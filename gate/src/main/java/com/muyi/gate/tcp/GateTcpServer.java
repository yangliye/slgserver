package com.muyi.gate.tcp;

import com.muyi.common.redis.RedisManager;
import com.muyi.gate.router.MessageRouter;
import com.muyi.gate.session.SessionManager;
import com.muyi.rpc.client.RpcProxyManager;
import com.muyi.rpc.transport.SharedEventLoopGroup;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Gate TCP 服务器
 * <p>
 * 面向客户端的 TCP 长连接服务器，复用 RPC 的 SharedEventLoopGroup。
 * <p>
 * 帧协议：4 字节 length + 4 字节 msgId + 4 字节 msgSeq + N 字节 payload
 *
 * @author muyi
 */
public class GateTcpServer {

    private static final Logger log = LoggerFactory.getLogger(GateTcpServer.class);

    private static final int MAX_FRAME_LENGTH = 65535;
    private static final int AUTH_TIMEOUT_SECONDS = 30;
    private static final int IDLE_TIMEOUT_SECONDS = 120;

    private final int port;
    private final int gateServerId;
    private final SessionManager sessionManager;
    private final RpcProxyManager rpcProxy;
    private final MessageRouter messageRouter;
    private final RedisManager globalRedis;

    private Channel serverChannel;
    private SharedEventLoopGroup sharedGroup;

    public GateTcpServer(int port, int gateServerId,
                         SessionManager sessionManager,
                         RpcProxyManager rpcProxy,
                         MessageRouter messageRouter,
                         RedisManager globalRedis) {
        this.port = port;
        this.gateServerId = gateServerId;
        this.sessionManager = sessionManager;
        this.rpcProxy = rpcProxy;
        this.messageRouter = messageRouter;
        this.globalRedis = globalRedis;
    }

    /**
     * 启动 TCP 服务器
     */
    public void start() throws InterruptedException {
        sharedGroup = SharedEventLoopGroup.getInstance().acquire();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(sharedGroup.bossGroup(), sharedGroup.workerGroup())
                .channel(sharedGroup.transport().serverChannelClass())
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast("idle", new IdleStateHandler(
                                        IDLE_TIMEOUT_SECONDS, 0, 0, TimeUnit.SECONDS))
                                .addLast("frameDecoder", new LengthFieldBasedFrameDecoder(
                                        MAX_FRAME_LENGTH, 0, 4, 0, 4))
                                .addLast("frameLengthEncoder", new LengthFieldPrepender(4))
                                .addLast("packetDecoder", new GamePacketDecoder())
                                .addLast("packetEncoder", new GamePacketEncoder())
                                .addLast("handler", new GateChannelHandler(
                                        sessionManager, rpcProxy, messageRouter,
                                        gateServerId, AUTH_TIMEOUT_SECONDS, globalRedis));
                    }
                });

        serverChannel = bootstrap.bind(port).sync().channel();
        log.info("Gate TCP server started on port {}", port);
    }

    /**
     * 停止 TCP 服务器
     */
    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
            log.info("Gate TCP server stopped");
        }
        if (sharedGroup != null) {
            sharedGroup.release();
        }
    }

    public int getPort() {
        return port;
    }
}
