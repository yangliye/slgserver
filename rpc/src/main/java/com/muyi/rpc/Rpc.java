package com.muyi.rpc;

import com.muyi.rpc.client.RpcClientConfig;
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
                            .clientConfig(new RpcClientConfig().requestTimeout(10_000).retries(1))
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
    
    // ==================== 包级访问方法（供 RpcBuilder 使用）====================
    
    static RpcServer serverInstance() {
        return server;
    }
    
    static void setHostInternal(String h) {
        host = h;
    }
    
    static void setWeightInternal(int w) {
        weight = w;
    }
}
