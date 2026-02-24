package com.muyi.rpc.client;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.muyi.rpc.codec.RpcDecoder;
import com.muyi.rpc.codec.RpcEncoder;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * 简单的连接池实现
 * 使用共享连接模式 - 所有请求共享固定数量的连接
 *
 * @author muyi
 */
public class ChannelPool {
    
    private static final Logger logger = LoggerFactory.getLogger(ChannelPool.class);
    
    private final String host;
    private final int port;
    private final int maxConnections;
    private final int initialConnections;
    /** 
     * 使用 AtomicReferenceArray 替代 CopyOnWriteArrayList
     * 避免写操作时复制整个数组，提升连接重建时的性能
     */
    private final AtomicReferenceArray<Channel> channels;
    /** 当前有效连接数 */
    private final AtomicInteger size = new AtomicInteger(0);
    private final AtomicInteger index = new AtomicInteger(0);
    private volatile boolean initialized = false;
    /** 是否已关闭（关闭后不允许再 acquire） */
    private volatile boolean closed = false;
    
    private final Bootstrap bootstrap;
    private final int heartbeatInterval;
    private final int heartbeatMaxFailCount;
    private final RpcClient rpcClient;
    
    public ChannelPool(String host, int port, RpcClientConfig config,
                       Bootstrap bootstrap, RpcClient rpcClient) {
        this.host = host;
        this.port = port;
        this.maxConnections = config.getMaxConnectionsPerAddress();
        this.initialConnections = config.getPoolInitialConnections();
        this.channels = new AtomicReferenceArray<>(maxConnections);
        this.bootstrap = bootstrap;
        this.heartbeatInterval = config.getHeartbeatInterval();
        this.heartbeatMaxFailCount = config.getHeartbeatMaxFailCount();
        this.rpcClient = rpcClient;
    }
    
    public Channel acquire() throws Exception {
        // 检查是否已关闭
        if (closed) {
            throw new Exception("ChannelPool is closed");
        }
        
        // 延迟初始化连接
        if (!initialized) {
            synchronized (this) {
                // 双重检查：在锁内再次检查 closed 状态
                if (closed) {
                    throw new Exception("ChannelPool is closed");
                }
                if (!initialized) {
                    initConnections();
                    initialized = true;
                }
            }
        }
        
        // 获取一个活跃的连接（轮询方式）
        int currentSize = size.get();
        if (currentSize == 0) {
            // 区分是 pool 已关闭还是初始化失败
            if (closed) {
                throw new Exception("ChannelPool is closed, no available channel");
            }
            throw new Exception("No available channel (pool not properly initialized)");
        }
        
        // 尝试次数为实际初始化的连接数（最多 maxConnections 次）
        int attempts = Math.min(currentSize, maxConnections);
        while (attempts-- > 0) {
            // 使用位运算确保非负，对 maxConnections 取模（channels 数组的实际大小）
            int idx = (index.getAndIncrement() & Integer.MAX_VALUE) % maxConnections;
            Channel channel = channels.get(idx);
            if (channel != null && channel.isActive()) {
                return channel;
            }
            // 连接失效或为 null，尝试重新创建（CAS 更新，避免并发问题）
            try {
                Channel newChannel = createChannel();
                if (newChannel != null) {
                    // 使用 CAS 操作替换失效连接
                    if (channels.compareAndSet(idx, channel, newChannel)) {
                        // 关闭旧连接
                        if (channel != null) {
                            channel.close();
                        } else {
                            // 新增连接（原槽位为空），增加 size 计数
                            size.incrementAndGet();
                        }
                        return newChannel;
                    } else {
                        // CAS 失败，说明其他线程已更新，关闭新创建的连接
                        newChannel.close();
                        // 重新获取当前槽位的连接
                        Channel current = channels.get(idx);
                        if (current != null && current.isActive()) {
                            return current;
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to recreate channel", e);
            }
        }
        
        throw new Exception("Failed to acquire channel");
    }
    
    private void initConnections() throws Exception {
        // 初始化时创建指定数量的连接
        int toCreate = Math.min(maxConnections, initialConnections);
        int created = 0;
        Exception lastException = null;
        
        for (int i = 0; i < toCreate; i++) {
            try {
                Channel channel = createChannel();
                if (channel != null) {
                    channels.set(i, channel);
                    created++;
                }
            } catch (Exception e) {
                lastException = e;
                logger.warn("Failed to create channel {}/{}", i + 1, toCreate, e);
            }
        }
        
        if (created == 0) {
            // 全部失败，清理已创建的连接（虽然此时 created=0 不需要清理，但保持代码健壮性）
            for (int i = 0; i < toCreate; i++) {
                Channel ch = channels.getAndSet(i, null);
                if (ch != null) {
                    ch.close();
                }
            }
            throw new Exception("Failed to initialize channel pool: no connections created", lastException);
        }
        size.set(created);
    }
    
    private Channel createChannel() throws Exception {
        ChannelFuture future = bootstrap.clone()
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new IdleStateHandler(0, heartbeatInterval, 0, TimeUnit.SECONDS));
                        pipeline.addLast(new RpcDecoder());
                        pipeline.addLast(new RpcEncoder());
                        pipeline.addLast(new ClientHeartbeatHandler(heartbeatMaxFailCount));
                        pipeline.addLast(new ClientHandler(rpcClient));
                    }
                })
                .connect(new InetSocketAddress(host, port));
        
        try {
            future.sync();
        } catch (InterruptedException e) {
            // 恢复中断状态
            Thread.currentThread().interrupt();
            throw new Exception("Connection interrupted to " + host + ":" + port, e);
        }
        
        if (future.isSuccess()) {
            logger.info("Connected to {}:{}", host, port);
            return future.channel();
        }
        
        // 连接失败，记录失败原因
        Throwable cause = future.cause();
        if (cause != null) {
            logger.warn("Failed to connect to {}:{} - {}", host, port, cause.getMessage());
        } else {
            logger.warn("Failed to connect to {}:{} - unknown reason", host, port);
        }
        return null;
    }
    
    public synchronized void close() {
        // 幂等检查，防止重复关闭
        if (closed) {
            return;
        }
        closed = true;
        initialized = false;
        
        // 遍历整个数组，确保关闭所有连接（而不仅仅是 size 范围内的）
        for (int i = 0; i < maxConnections; i++) {
            Channel channel = channels.getAndSet(i, null);
            if (channel != null) {
                // 使用 addListener 避免在 EventLoop 线程中 sync() 导致死锁
                channel.close().addListener(future -> {
                    if (!future.isSuccess()) {
                        logger.warn("Failed to close channel", future.cause());
                    }
                });
            }
        }
        size.set(0);
    }
}

