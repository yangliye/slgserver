package com.muyi.rpc;

import com.muyi.rpc.client.RpcProxyManager;
import com.muyi.rpc.compress.Compressor;
import com.muyi.rpc.compress.CompressorFactory;
import com.muyi.rpc.compress.GzipCompressor;
import com.muyi.rpc.registry.ZookeeperServiceRegistry;
import com.muyi.rpc.serialize.Serializer;
import com.muyi.rpc.serialize.SerializerFactory;
import com.muyi.rpc.server.RpcServer;

/**
 * RPC 框架统一入口
 * 提供最简洁的 API 来启动服务端和客户端
 * 
 * 使用示例:
 * <pre>
 * // ========== 服务端（推荐写法）==========
 * // 1. 初始化
 * Rpc.init(8001, 1, "localhost:2181");
 * 
 * // 2. 分散注册服务（可以在不同地方调用）
 * Rpc.register(gameService);
 * Rpc.register(inventoryService);
 * Rpc.register(chatService);
 * 
 * // 3. 启动
 * Rpc.start();
 * 
 * // 或者一行搞定：
 * Rpc.init(8001, 1, "localhost:2181")
 *    .register(gameService)
 *    .register(inventoryService)
 *    .start();
 * 
 * // ========== 客户端 ==========
 * RpcProxyManager rpc = Rpc.connect("localhost:2181");
 * IGameService game = rpc.get(IGameService.class, 1);
 * 
 * // ========== 自定义序列化 ==========
 * Rpc.serializer(new JsonSerializer());  // 注册并设为默认
 * 
 * // ========== 启用压缩 ==========
 * Rpc.enableGzip();                      // 启用 Gzip 压缩
 * Rpc.compressThreshold(2048);           // 设置压缩阈值（字节）
 * 
 * // ========== 关闭 ==========
 * Rpc.shutdown();
 * </pre>
 *
 * @author muyi
 */
public final class Rpc {
    
    /** 全局服务端实例 */
    private static volatile RpcServer server;
    private static volatile ZookeeperServiceRegistry serverRegistry;
    
    /** 全局客户端实例 */
    private static volatile RpcProxyManager client;
    private static volatile ZookeeperServiceRegistry clientRegistry;
    
    /** 初始化配置 */
    private static int port;
    private static int serverId;
    private static String zkAddress;
    private static String host;
    private static int weight = 100;
    
    private Rpc() {}
    
    // ==================== 序列化配置 ====================
    
    /**
     * 注册自定义序列化器并设为默认
     *
     * @param serializer 序列化器实现
     */
    public static void serializer(Serializer serializer) {
        SerializerFactory.register(serializer);
        SerializerFactory.setDefaultType(serializer.getType());
    }
    
    /**
     * 注册自定义序列化器（不设为默认）
     *
     * @param serializer 序列化器实现
     */
    public static void registerSerializer(Serializer serializer) {
        SerializerFactory.register(serializer);
    }
    
    /**
     * 设置默认序列化器类型
     *
     * @param type 序列化器类型
     */
    public static void setDefaultSerializer(byte type) {
        SerializerFactory.setDefaultType(type);
    }
    
    // ==================== 压缩配置 ====================
    
    /**
     * 启用 Gzip 压缩
     * 适合大数据量传输，压缩率高
     */
    public static void enableGzip() {
        CompressorFactory.setDefaultType(GzipCompressor.TYPE);
    }
    
    /**
     * 禁用压缩
     */
    public static void disableCompress() {
        CompressorFactory.setDefaultType(Compressor.NONE);
    }
    
    /**
     * 注册自定义压缩器并设为默认
     *
     * @param compressor 压缩器实现
     */
    public static void compressor(Compressor compressor) {
        CompressorFactory.register(compressor);
        CompressorFactory.setDefaultType(compressor.getType());
    }
    
    /**
     * 注册自定义压缩器（不设为默认）
     *
     * @param compressor 压缩器实现
     */
    public static void registerCompressor(Compressor compressor) {
        CompressorFactory.register(compressor);
    }
    
    /**
     * 设置压缩阈值（字节）
     * 数据大小超过此阈值才进行压缩，默认 1KB
     *
     * @param threshold 阈值（字节）
     */
    public static void compressThreshold(int threshold) {
        CompressorFactory.setCompressThreshold(threshold);
    }
    
    // ==================== 服务端 API ====================
    
    /**
     * 初始化服务端配置
     *
     * @param port      服务端口
     * @param serverId  服务器ID
     * @param zkAddress ZooKeeper 地址
     * @return Rpc 用于链式调用
     */
    public static RpcBuilder init(int port, int serverId, String zkAddress) {
        Rpc.port = port;
        Rpc.serverId = serverId;
        Rpc.zkAddress = zkAddress;
        return new RpcBuilder();
    }
    
    /**
     * 注册服务（可多次调用，分散注册）
     *
     * @param service 服务实现（带 @RpcService 注解）
     */
    public static RpcBuilder register(Object service) {
        ensureServerCreated();
        server.registerService(service);
        return new RpcBuilder();
    }
    
    /**
     * 启动服务端
     */
    public static void start() throws Exception {
        ensureServerCreated();
        if (!server.isRunning()) {
            server.start();
        }
    }
    
    /**
     * 获取全局服务端实例
     */
    public static RpcServer getServer() {
        return server;
    }
    
    // ==================== 客户端 API ====================
    
    /** 全局客户端连接的 ZK 地址 */
    private static volatile String clientZkAddress;
    
    /**
     * 获取 RPC 客户端（懒加载，全局单例）
     * 注意：如果传入不同的 zkAddress，会返回已有的 client 并记录警告日志
     *
     * @param zkAddress ZooKeeper 地址
     * @return RpcProxyManager 用于获取服务代理
     */
    public static RpcProxyManager connect(String zkAddress) {
        if (client == null) {
            synchronized (Rpc.class) {
                if (client == null) {
                    clientZkAddress = zkAddress;
                    clientRegistry = new ZookeeperServiceRegistry(zkAddress);
                    client = new RpcProxyManager()
                            .discovery(clientRegistry)
                            .requestTimeout(10_000)
                            .retries(1)
                            .init();
                }
            }
        } else if (!zkAddress.equals(clientZkAddress)) {
            // 警告：传入的 zkAddress 与已创建的 client 不同
            org.slf4j.LoggerFactory.getLogger(Rpc.class)
                    .warn("Rpc.connect() called with different zkAddress '{}', but client already exists for '{}'. " +
                          "Returning existing client. Use Rpc.client().zookeeper(\"{}\").connect() to create a new instance.",
                          zkAddress, clientZkAddress, zkAddress);
        }
        return client;
    }
    
    /**
     * 获取全局客户端实例
     */
    public static RpcProxyManager getClient() {
        return client;
    }
    
    // ==================== 生命周期 ====================
    
    /**
     * 关闭所有资源（线程安全）
     */
    public static synchronized void shutdown() {
        if (server != null) {
            server.shutdown();
            server = null;
        }
        if (serverRegistry != null) {
            serverRegistry.shutdown();
            serverRegistry = null;
        }
        if (client != null) {
            client.shutdown();
            client = null;
        }
        if (clientRegistry != null) {
            clientRegistry.shutdown();
            clientRegistry = null;
        }
        clientZkAddress = null;
        
        // 重置静态配置，避免再次 init 时状态混乱
        port = 0;
        serverId = 0;
        zkAddress = null;
        host = null;
        weight = 100;
        
        // 关闭 ServerHandler 的共享线程池（全局关闭时调用）
        com.muyi.rpc.server.ServerHandler.shutdownExecutor();
    }
    
    // ==================== 内部方法 ====================
    
    private static void ensureServerCreated() {
        if (server == null) {
            synchronized (Rpc.class) {
                if (server == null) {
                    if (zkAddress == null) {
                        throw new IllegalStateException("Must call Rpc.init() first");
                    }
                    serverRegistry = new ZookeeperServiceRegistry(zkAddress);
                    serverRegistry.setWeight(weight);
                    serverRegistry.setServerId(String.valueOf(serverId));
                    
                    server = new RpcServer(port)
                            .registry(serverRegistry)
                            .serverId(serverId);
                    
                    if (host != null) {
                        server.host(host);
                    }
                }
            }
        }
    }
    
    // ==================== 链式调用 Builder ====================
    
    public static class RpcBuilder {
        
        /** 注册服务 */
        public RpcBuilder register(Object service) {
            Rpc.register(service);
            return this;
        }
        
        /** 
         * 设置主机地址
         * 注意：必须在 register() 之前调用，否则不会生效
         */
        public RpcBuilder host(String host) {
            if (server != null) {
                throw new IllegalStateException("host() must be called before register(). Server already created.");
            }
            Rpc.host = host;
            return this;
        }
        
        /** 
         * 设置权重
         * 注意：必须在 register() 之前调用，否则不会生效
         */
        public RpcBuilder weight(int weight) {
            if (server != null) {
                throw new IllegalStateException("weight() must be called before register(). Server already created.");
            }
            Rpc.weight = weight;
            return this;
        }
        
        /** 启动服务端 */
        public void start() throws Exception {
            Rpc.start();
        }
    }
    
    // ==================== 客户端连接包装类 ====================
    
    /**
     * 客户端连接包装类，包含 RpcProxyManager 和 ZookeeperServiceRegistry
     * 用于统一管理资源关闭
     */
    public static class ClientConnection implements AutoCloseable {
        private final RpcProxyManager proxyManager;
        private final ZookeeperServiceRegistry registry;
        
        ClientConnection(RpcProxyManager proxyManager, ZookeeperServiceRegistry registry) {
            this.proxyManager = proxyManager;
            this.registry = registry;
        }
        
        public RpcProxyManager getProxyManager() {
            return proxyManager;
        }
        
        public ZookeeperServiceRegistry getRegistry() {
            return registry;
        }
        
        @Override
        public void close() {
            if (proxyManager != null) {
                proxyManager.shutdown();
            }
            if (registry != null) {
                registry.shutdown();
            }
        }
        
        public void shutdown() {
            close();
        }
    }
    
    // ==================== 高级 API（独立实例）====================
    
    /**
     * 创建独立的服务端 Builder（不使用全局实例）
     */
    public static ServerBuilder server(int port) {
        return new ServerBuilder(port);
    }
    
    /**
     * 创建独立的客户端 Builder（不使用全局实例）
     */
    public static ClientBuilder client() {
        return new ClientBuilder();
    }
    
    // ==================== 独立服务端 Builder ====================
    
    public static class ServerBuilder {
        private final int port;
        private int serverId = 1;
        private String zkAddress;
        private String host;
        private int weight = 100;
        private int idleTimeout = 60;
        private RpcServer server;
        private ZookeeperServiceRegistry registry;
        
        ServerBuilder(int port) {
            this.port = port;
        }
        
        public ServerBuilder serverId(int serverId) {
            this.serverId = serverId;
            return this;
        }
        
        public ServerBuilder zookeeper(String zkAddress) {
            this.zkAddress = zkAddress;
            return this;
        }
        
        public ServerBuilder host(String host) {
            this.host = host;
            return this;
        }
        
        public ServerBuilder weight(int weight) {
            this.weight = weight;
            return this;
        }
        
        public ServerBuilder idleTimeout(int seconds) {
            this.idleTimeout = seconds;
            return this;
        }
        
        /** 注册服务（可链式调用多次）*/
        public ServerBuilder register(Object service) {
            ensureCreated();
            server.registerService(service);
            return this;
        }
        
        /** 启动服务 */
        public RpcServer start() throws Exception {
            ensureCreated();
            server.start();
            return server;
        }
        
        private void ensureCreated() {
            if (server == null) {
                if (zkAddress == null) {
                    throw new IllegalStateException("ZooKeeper address is required. Call zookeeper() first.");
                }
                registry = new ZookeeperServiceRegistry(zkAddress);
                registry.setWeight(weight);
                registry.setServerId(String.valueOf(serverId));
                
                server = new RpcServer(port)
                        .registry(registry)
                        .serverId(serverId)
                        .readerIdleTime(idleTimeout);
                
                if (host != null) {
                    server.host(host);
                }
            }
        }
    }
    
    // ==================== 独立客户端 Builder ====================
    
    public static class ClientBuilder {
        private String zkAddress;
        private long timeout = 10_000;
        private int retries = 1;
        private int connectTimeout = 3_000;
        private int maxConnections = 10;
        
        ClientBuilder() {}
        
        public ClientBuilder zookeeper(String zkAddress) {
            this.zkAddress = zkAddress;
            return this;
        }
        
        public ClientBuilder timeout(long timeout) {
            this.timeout = timeout;
            return this;
        }
        
        public ClientBuilder retries(int retries) {
            this.retries = retries;
            return this;
        }
        
        public ClientBuilder connectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }
        
        public ClientBuilder maxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
            return this;
        }
        
        public RpcProxyManager connect() {
            if (zkAddress == null) {
                throw new IllegalStateException("ZooKeeper address is required. Call zookeeper() first.");
            }
            
            ZookeeperServiceRegistry registry = new ZookeeperServiceRegistry(zkAddress);
            
            RpcProxyManager manager = new RpcProxyManager()
                    .discovery(registry)
                    .requestTimeout(timeout)
                    .retries(retries)
                    .connectTimeout(connectTimeout)
                    .maxConnectionsPerAddress(maxConnections)
                    .init();
            
            // 保存 registry 引用，在 manager shutdown 时会通过 discovery 的 shutdown 方法关闭
            // 注意：RpcProxyManager.shutdown() 不会关闭 discovery，需要手动调用 registry.shutdown()
            // 返回包装对象或文档说明用户需要手动关闭 registry
            // 这里记录日志提醒用户
            org.slf4j.LoggerFactory.getLogger(Rpc.class)
                    .info("Created independent RpcProxyManager. Remember to call registry.shutdown() when done.");
            
            return manager;
        }
        
        /**
         * 创建客户端并返回包含 registry 的包装对象，方便统一关闭
         */
        public ClientConnection connectWithRegistry() {
            if (zkAddress == null) {
                throw new IllegalStateException("ZooKeeper address is required. Call zookeeper() first.");
            }
            
            ZookeeperServiceRegistry registry = new ZookeeperServiceRegistry(zkAddress);
            
            RpcProxyManager manager = new RpcProxyManager()
                    .discovery(registry)
                    .requestTimeout(timeout)
                    .retries(retries)
                    .connectTimeout(connectTimeout)
                    .maxConnectionsPerAddress(maxConnections)
                    .init();
            
            return new ClientConnection(manager, registry);
        }
    }
}
