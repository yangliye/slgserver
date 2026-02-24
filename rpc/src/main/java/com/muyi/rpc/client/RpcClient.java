package com.muyi.rpc.client;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.muyi.rpc.client.loadbalance.LoadBalance;
import com.muyi.rpc.client.loadbalance.RandomLoadBalance;
import com.muyi.rpc.core.RpcFuture;
import com.muyi.rpc.core.RpcRequest;
import com.muyi.rpc.core.RpcResponse;
import com.muyi.rpc.protocol.MessageType;
import com.muyi.rpc.protocol.RpcMessage;
import com.muyi.rpc.registry.ServiceDiscovery;
import com.muyi.rpc.serialize.SerializerFactory;
import com.muyi.rpc.transport.TransportType;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;

/**
 * RPC客户端
 * 支持连接池、负载均衡、心跳检测
 *
 * @author muyi
 */
public class RpcClient {

    private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);

    /**
     * 服务发现
     */
    private ServiceDiscovery discovery;

    /**
     * 负载均衡策略
     */
    private LoadBalance loadBalance = new RandomLoadBalance();

    /**
     * 连接池
     */
    private final Map<String, ChannelPool> channelPools = new ConcurrentHashMap<>();

    /**
     * 等待响应的Future
     */
    private final Map<Long, RpcFuture> pendingFutures = new ConcurrentHashMap<>();

    /**
     * EventLoopGroup
     */
    private final EventLoopGroup eventLoopGroup;

    /**
     * Bootstrap
     */
    private final Bootstrap bootstrap;

    /**
     * 客户端配置
     */
    private final RpcClientConfig config;

    /**
     * 时间轮定时器（用于请求超时检测）
     * 相比 ScheduledExecutorService 遍历检查，时间轮复杂度 O(1)，更适合大量定时任务
     */
    private final Timer wheelTimer;

    /**
     * 是否已关闭（使用 AtomicBoolean 确保线程安全）
     */
    private final java.util.concurrent.atomic.AtomicBoolean shutdown = new java.util.concurrent.atomic.AtomicBoolean(false);

    public RpcClient() {
        this(new RpcClientConfig());
    }

    public RpcClient(RpcClientConfig config) {
        this.config = config;

        TransportType transport = TransportType.detect();
        
        this.eventLoopGroup = transport.createEventLoopGroup(0);
        this.bootstrap = new Bootstrap();

        bootstrap.group(eventLoopGroup)
                .channel(transport.socketChannelClass())
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectTimeout())
                .option(ChannelOption.SO_SNDBUF, config.getSendBufferSize())
                .option(ChannelOption.SO_RCVBUF, config.getReceiveBufferSize())
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.WRITE_BUFFER_WATER_MARK, 
                        new WriteBufferWaterMark(config.getWriteLowWaterMark(), config.getWriteHighWaterMark()));

        logger.info("RpcClient using transport: {} ({})", transport.name(), transport.description());

        wheelTimer = new HashedWheelTimer(
                Thread.ofVirtual().name("rpc-wheel-timer-", 0).factory(),
                config.getWheelTimerTickMs(), TimeUnit.MILLISECONDS,
                config.getWheelTimerTicks()
        );
    }

    /**
     * 同步调用（指定超时时间）
     * 注意：内部使用无参 get()，依赖 invokeAsync 中设置的超时任务
     */
    public Object invoke(RpcRequest request, long timeout) throws Exception {
        checkNotShutdown();
        RpcFuture future = invokeAsync(request, timeout);
        // 使用无参 get()，它内部会使用 RpcFuture 构造时传入的 timeout
        // 避免与 invokeAsync 中的超时任务叠加导致等待时间翻倍
        return future.get();
    }

    /**
     * 异步调用（指定超时时间）
     */
    public RpcFuture invokeAsync(RpcRequest request, long timeout) {
        // 检查状态
        if (shutdown.get()) {
            RpcFuture future = new RpcFuture(request.getRequestId(), timeout);
            future.complete(RpcResponse.fail(request.getRequestId(), "RpcClient is already shutdown"));
            return future;
        }
        
        // 创建Future
        RpcFuture future = new RpcFuture(request.getRequestId(), timeout);
        pendingFutures.put(request.getRequestId(), future);
        
        // 注册超时任务到时间轮（O(1) 复杂度，比遍历检查高效）
        Timeout timeoutTask = wheelTimer.newTimeout(t -> {
            RpcFuture f = pendingFutures.remove(request.getRequestId());
            if (f != null && !f.isDone()) {
                f.complete(RpcResponse.fail(request.getRequestId(), "Request timeout"));
            }
        }, timeout, TimeUnit.MILLISECONDS);
        future.setTimeoutTask(timeoutTask);

        try {
            // 获取服务地址
            String address = selectAddress(request.getServiceKey());
            if (address == null) {
                timeoutTask.cancel(); // 取消超时任务
                future.complete(RpcResponse.fail(request.getRequestId(), "No available service"));
                pendingFutures.remove(request.getRequestId());
                return future;
            }

            // 获取连接
            Channel channel = getChannel(address);
            if (channel == null || !channel.isActive()) {
                timeoutTask.cancel(); // 取消超时任务
                future.complete(RpcResponse.fail(request.getRequestId(), "Failed to get connection"));
                pendingFutures.remove(request.getRequestId());
                return future;
            }

            // 构建消息
            RpcMessage message = new RpcMessage(
                    MessageType.REQUEST,
                    SerializerFactory.getDefaultType(),
                    request.getRequestId(),
                    request
            );

            // 发送请求
            channel.writeAndFlush(message).addListener((ChannelFutureListener) f -> {
                if (!f.isSuccess()) {
                    future.cancelTimeoutTask(); // 取消超时任务
                    future.complete(RpcResponse.fail(request.getRequestId(), "Send request failed"));
                    pendingFutures.remove(request.getRequestId());
                }
            });

        } catch (Exception e) {
            timeoutTask.cancel(); // 取消超时任务
            future.complete(RpcResponse.error(request.getRequestId(), e));
            pendingFutures.remove(request.getRequestId());
        }

        return future;
    }

    /**
     * 单向调用（不等待响应）
     */
    public void invokeOneWay(RpcRequest request) {
        if (shutdown.get()) {
            logger.warn("RpcClient is shutdown, ignore one-way request: {}", request.getServiceKey());
            return;
        }
        
        try {
            request.setOneWay(true);

            String address = selectAddress(request.getServiceKey());
            if (address == null) {
                logger.warn("No available service for: {}", request.getServiceKey());
                return;
            }

            Channel channel = getChannel(address);
            if (channel == null || !channel.isActive()) {
                logger.warn("Failed to get connection for: {}", address);
                return;
            }

            RpcMessage message = new RpcMessage(
                    MessageType.REQUEST,
                    SerializerFactory.getDefaultType(),
                    request.getRequestId(),
                    request
            );

            channel.writeAndFlush(message);

        } catch (Exception e) {
            logger.error("One-way invoke error", e);
        }
    }

    /**
     * 选择服务地址
     */
    private String selectAddress(String serviceKey) {
        if (discovery != null) {
            List<String> addresses = discovery.discover(serviceKey);
            if (addresses == null || addresses.isEmpty()) {
                return null;
            }
            return loadBalance.select(addresses, serviceKey);
        }
        return null;
    }

    /**
     * 获取Channel连接
     */
    private Channel getChannel(String address) throws Exception {
        ChannelPool pool = channelPools.computeIfAbsent(address, this::createChannelPool);
        return pool.acquire();
    }

    /**
     * 创建连接池
     */
    private ChannelPool createChannelPool(String address) {
        // 校验地址格式
        int colonIndex = address.lastIndexOf(':');
        if (colonIndex <= 0 || colonIndex == address.length() - 1) {
            throw new IllegalArgumentException("Invalid address format, expected host:port, got: " + address);
        }
        
        String host = address.substring(0, colonIndex);
        int port;
        try {
            port = Integer.parseInt(address.substring(colonIndex + 1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port in address: " + address, e);
        }
        
        return new ChannelPool(host, port, config, bootstrap, this);
    }

    /**
     * 处理响应
     */
    void handleResponse(RpcResponse response) {
        RpcFuture future = pendingFutures.remove(response.getRequestId());
        if (future != null) {
            // 取消超时任务（响应已到达，无需再触发超时）
            future.cancelTimeoutTask();
            future.complete(response);
        } else {
            logger.warn("No pending future for response: {}", response.getRequestId());
        }
    }

    /**
     * 关闭客户端（线程安全，只执行一次）
     */
    public void shutdown() {
        // CAS 确保只执行一次
        if (!shutdown.compareAndSet(false, true)) {
            return;
        }

        logger.info("Shutting down RPC Client...");

        // 关闭时间轮定时器
        wheelTimer.stop();

        // 关闭所有连接池
        for (ChannelPool pool : channelPools.values()) {
            pool.close();
        }
        channelPools.clear();

        // 取消所有等待的请求
        for (RpcFuture future : pendingFutures.values()) {
            future.complete(RpcResponse.fail(future.getRequestId(), "Client shutdown"));
        }
        pendingFutures.clear();

        try {
            eventLoopGroup.shutdownGracefully().await(config.getShutdownTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while waiting for eventLoopGroup shutdown");
        }

        logger.info("RPC Client shutdown complete");
    }

    /**
     * 检查客户端是否已关闭
     */
    public boolean isShutdown() {
        return shutdown.get();
    }
    
    // Builder方法
    
    /**
     * 检查客户端是否可用（内部使用）
     */
    private void checkNotShutdown() {
        if (shutdown.get()) {
            throw new IllegalStateException("RpcClient is already shutdown");
        }
    }

    public RpcClient discovery(ServiceDiscovery discovery) {
        checkNotShutdown();
        this.discovery = discovery;
        return this;
    }

    public RpcClient loadBalance(LoadBalance loadBalance) {
        checkNotShutdown();
        this.loadBalance = loadBalance;
        return this;
    }

    public RpcClientConfig getConfig() {
        return config;
    }
}
