package com.muyi.rpc.server;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.muyi.rpc.annotation.RpcService;
import com.muyi.rpc.codec.RpcDecoder;
import com.muyi.rpc.codec.RpcEncoder;
import com.muyi.rpc.core.ServiceKey;
import com.muyi.rpc.registry.ServiceRegistry;
import com.muyi.rpc.transport.TransportType;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * RPC服务端
 * 基于Netty实现高性能网络通信
 * 
 * 服务标识格式：interfaceName#serverId
 * 示例：com.example.IGameService#1
 *
 * @author muyi
 */
public class RpcServer {
    
    private static final Logger logger = LoggerFactory.getLogger(RpcServer.class);
    
    /** 服务端口 */
    private final int port;
    
    /** 服务注册中心 */
    private ServiceRegistry registry;
    
    /** 服务实例缓存 */
    private final Map<String, Object> serviceMap = new ConcurrentHashMap<>();
    
    /** Boss线程组 */
    private EventLoopGroup bossGroup;
    
    /** Worker线程组 */
    private EventLoopGroup workerGroup;
    
    /** 服务端Channel */
    private Channel serverChannel;
    
    /** 读超时时间（秒） */
    private int readerIdleTime = 60;
    
    /** 服务端地址（注册到注册中心的地址） */
    private String serverAddress;
    
    /** 自定义主机地址（IP或域名），为null时自动获取本机IP */
    private String host;
    
    /** 服务器ID */
    private int serverId;
    
    /** 是否已启动（使用 AtomicBoolean 确保线程安全） */
    private final java.util.concurrent.atomic.AtomicBoolean running = new java.util.concurrent.atomic.AtomicBoolean(false);
    
    /** 是否已关闭（用于防止重复关闭和异常时的资源清理） */
    private final java.util.concurrent.atomic.AtomicBoolean closed = new java.util.concurrent.atomic.AtomicBoolean(false);
    
    public RpcServer(int port) {
        this.port = port;
    }
    
    public RpcServer(int port, ServiceRegistry registry) {
        this.port = port;
        this.registry = registry;
    }
    
    /**
     * 注册服务
     * 注意：如果服务端已启动，新注册的服务会自动推送到注册中心
     *
     * @param service 服务实例
     */
    public void registerService(Object service) {
        String serviceKey = getServiceKey(service);

        // 检查是否重复注册
        Object existing = serviceMap.put(serviceKey, service);
        if (existing != null) {
            logger.warn("Service {} already registered, replacing with new instance", serviceKey);
        }
        
        logger.info("Register service: {} (serverId={})", serviceKey, serverId);
        
        // 如果服务端已启动，自动注册到注册中心
        if (running.get() && registry != null && serverAddress != null) {
            try {
                registry.register(serviceKey, serverAddress);
                logger.info("Service registered to registry: {}", serviceKey);
            } catch (Exception e) {
                logger.error("Failed to register service to registry: {}", serviceKey, e);
            }
        }
    }

    private @NonNull String getServiceKey(Object service) {
        Class<?> clazz = service.getClass();
        RpcService annotation = clazz.getAnnotation(RpcService.class);

        String interfaceName;

        if (annotation != null) {
            Class<?> interfaceClass = annotation.value();
            if (interfaceClass == void.class) {
                Class<?>[] interfaces = clazz.getInterfaces();
                if (interfaces.length == 0) {
                    throw new IllegalArgumentException("Service must implement an interface");
                }
                interfaceClass = interfaces[0];
            }
            interfaceName = interfaceClass.getName();
        } else {
            Class<?>[] interfaces = clazz.getInterfaces();
            if (interfaces.length == 0) {
                throw new IllegalArgumentException("Service must implement an interface");
            }
            interfaceName = interfaces[0].getName();
        }

        return ServiceKey.build(interfaceName, serverId);
    }

    /**
     * 获取服务实例
     */
    public Object getService(String serviceKey) {
        return serviceMap.get(serviceKey);
    }
    
    /**
     * 启动服务端
     */
    public void start() throws Exception {
        // 防止重复启动
        if (running.get()) {
            logger.warn("RPC Server [{}] is already running, skip start", serverId);
            return;
        }
        if (closed.get()) {
            throw new IllegalStateException("RPC Server [" + serverId + "] has been shutdown, cannot restart");
        }
        
        // 根据平台选择最优实现
        TransportType transport = TransportType.detect();
        Class<? extends ServerChannel> channelClass = transport.serverChannelClass();
        
        bossGroup = transport.createEventLoopGroup(1);
        workerGroup = transport.createEventLoopGroup(0);
        
        logger.info("Using transport: {} ({})", transport.name(), transport.description());
        
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(channelClass)
                    
                    // ==================== 服务端 Socket 参数（option）====================
                    // SO_BACKLOG: 全连接队列大小，等待 accept 的连接数
                    // 高并发时需要调大，否则客户端可能收到 connection refused
                    .option(ChannelOption.SO_BACKLOG, 4096)
                    // SO_REUSEADDR: 允许重用 TIME_WAIT 状态的地址
                    // 服务重启时可立即绑定端口，不用等待 2MSL
                    .option(ChannelOption.SO_REUSEADDR, true)
                    // 使用池化的 ByteBuf 分配器，减少 GC 压力
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    
                    // ==================== 客户端连接参数（childOption）====================
                    // SO_KEEPALIVE: TCP 心跳保活，检测死连接（默认 2 小时探测一次）
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // TCP_NODELAY: 禁用 Nagle 算法，小包立即发送，降低延迟
                    // 游戏/RPC 场景必开，否则会有 40ms 延迟
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    // SO_SNDBUF: 发送缓冲区大小（256KB）
                    .childOption(ChannelOption.SO_SNDBUF, 256 * 1024)
                    // SO_RCVBUF: 接收缓冲区大小（256KB）
                    .childOption(ChannelOption.SO_RCVBUF, 256 * 1024)
                    // 使用池化的 ByteBuf 分配器
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    // 写缓冲区水位线：低水位 64KB，高水位 128KB
                    // 超过高水位时 channel.isWritable() 返回 false，防止 OOM
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, 
                            new WriteBufferWaterMark(64 * 1024, 128 * 1024));
            
            // Linux Epoll 专用优化
            if (transport.isEpoll()) {
                bootstrap
                        .option(EpollChannelOption.SO_REUSEPORT, true)      // 端口复用（多进程负载均衡）
                        .childOption(EpollChannelOption.TCP_QUICKACK, true); // 快速 ACK
            }
            
            bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast(new IdleStateHandler(readerIdleTime, 0, 0, TimeUnit.SECONDS));
                    pipeline.addLast(new RpcDecoder());
                    pipeline.addLast(new RpcEncoder());
                    pipeline.addLast(new ServerHeartbeatHandler());
                    pipeline.addLast(new ServerHandler(RpcServer.this));
                }
            });
            
            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();
            
            serverAddress = resolveServerAddress();
            
            // 注册服务到注册中心
            if (registry != null) {
                for (String serviceKey : serviceMap.keySet()) {
                    registry.register(serviceKey, serverAddress);
                }
            }
            
            running.set(true);
            logger.info("RPC Server [{}] started on {} using {}", serverId, serverAddress, transport.name());
            
            future.channel().closeFuture().addListener((ChannelFutureListener) f -> {
                running.set(false);
                logger.info("RPC Server [{}] channel closed", serverId);
            });
            
        } catch (Exception e) {
            logger.error("RPC Server start error", e);
            shutdown();
            throw e;
        }
    }
    
    /**
     * 解析服务端地址
     */
    private String resolveServerAddress() {
        String resolvedHost;
        
        if (host != null && !host.isEmpty()) {
            resolvedHost = host;
        } else {
            try {
                resolvedHost = InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                logger.warn("Failed to get local host address, using 127.0.0.1", e);
                resolvedHost = "127.0.0.1";
            }
        }
        
        return resolvedHost + ":" + port;
    }
    
    /**
     * 同步启动（阻塞）
     */
    public void startAndWait() throws Exception {
        start();
        // 检查 serverChannel 是否创建成功
        if (serverChannel != null) {
            serverChannel.closeFuture().sync();
        }
    }
    
    /**
     * 关闭服务端（线程安全，只执行一次）
     */
    public void shutdown() {
        // CAS 确保只执行一次（使用 closed 标志而非 running，以处理启动失败的情况）
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        
        running.set(false);
        logger.info("Shutting down RPC Server [{}]...", serverId);
        
        if (registry != null && serverAddress != null) {
            for (String serviceKey : serviceMap.keySet()) {
                try {
                    registry.unregister(serviceKey, serverAddress);
                } catch (Exception e) {
                    logger.error("Unregister service error: {}", serviceKey, e);
                }
            }
        }
        
        if (serverChannel != null) {
            serverChannel.close();
        }
        
        // 优雅关闭线程组，等待完成（最多等待 15 秒）
        if (bossGroup != null) {
            try {
                bossGroup.shutdownGracefully().await(15, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while waiting for bossGroup shutdown");
            }
        }
        if (workerGroup != null) {
            try {
                workerGroup.shutdownGracefully().await(15, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while waiting for workerGroup shutdown");
            }
        }
        
        logger.info("RPC Server [{}] shutdown complete", serverId);
    }
    
    /**
     * 是否已启动
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * 是否已关闭
     */
    public boolean isClosed() {
        return closed.get();
    }
    
    // ==================== Builder模式配置 ====================
    
    /**
     * 检查是否可以修改配置（启动前才允许）
     */
    private void checkNotStarted() {
        if (running.get()) {
            throw new IllegalStateException("Cannot modify configuration after server started");
        }
    }
    
    public RpcServer registry(ServiceRegistry registry) {
        checkNotStarted();
        this.registry = registry;
        return this;
    }
    
    public RpcServer readerIdleTime(int seconds) {
        checkNotStarted();
        this.readerIdleTime = seconds;
        return this;
    }
    
    /**
     * 设置注册到注册中心的主机地址
     */
    public RpcServer host(String host) {
        checkNotStarted();
        this.host = host;
        return this;
    }
    
    /**
     * 设置服务器ID
     * 用于区分同类型服务器的不同实例（如 1, 2, 3）
     */
    public RpcServer serverId(int serverId) {
        checkNotStarted();
        this.serverId = serverId;
        return this;
    }
    
    /**
     * 设置完整的服务地址（host:port）
     */
    public RpcServer address(String address) {
        checkNotStarted();
        this.serverAddress = address;
        int colonIndex = address.lastIndexOf(':');
        if (colonIndex > 0) {
            this.host = address.substring(0, colonIndex);
        }
        return this;
    }
    
    public int getPort() {
        return port;
    }
    
    public String getServerAddress() {
        return serverAddress;
    }
    
    public String getHost() {
        return host;
    } 
    
    public int getServerId() {
        return serverId;
    }
}
