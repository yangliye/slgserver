package com.muyi.core.module;

import com.muyi.common.redis.RedisManager;
import com.muyi.core.config.ModuleConfig;
import com.muyi.core.web.WebServer;
import com.muyi.db.DbManager;
import com.muyi.db.config.DbConfig;
import com.muyi.rpc.client.RpcClientConfig;
import com.muyi.rpc.client.RpcProxyManager;
import com.muyi.rpc.registry.ZookeeperServiceRegistry;
import com.muyi.rpc.server.RpcServer;
import com.muyi.rpc.server.RpcServerConfig;
import com.muyi.rpc.transport.SharedEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 游戏模块抽象基类
 * 提供通用的 RPC 服务和 Web 服务管理
 *
 * @author muyi
 */
public abstract class AbstractGameModule implements GameModule {
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    protected ModuleConfig config;
    protected RpcServer rpcServer;
    protected RpcProxyManager rpcProxyManager;
    protected WebServer webServer;
    protected DbManager dbManager;
    private ZookeeperServiceRegistry zkRegistry;
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    @Override
    public void init(ModuleConfig config) {
        this.config = config;
        log.info("[{}] Initializing module...", name());
        
        // 初始化 ZooKeeper 注册中心（Server 和 Client 共用）
        String zkAddr = config.getZkAddress();
        if (zkAddr != null && !zkAddr.isEmpty()) {
            this.zkRegistry = new ZookeeperServiceRegistry(
                    zkAddr,
                    config.getZkSessionTimeout(),
                    config.getZkConnectionTimeout(),
                    "",
                    config.getZkRetryInitialDelay(),
                    config.getZkRetryMaxRetries(),
                    config.getZkRetryMaxDelay());
        }
        
        // 初始化 RPC 服务端
        int rpcPort = rpcPort();
        if (rpcPort > 0) {
            this.rpcServer = new RpcServer(rpcPort, buildRpcServerConfig(config));
            rpcServer.serverId(config.getServerId());
            rpcServer.sharedEventLoopGroup(SharedEventLoopGroup.getInstance());
            if (config.getHost() != null) {
                rpcServer.host(config.getHost());
            }
            if (zkRegistry != null) {
                rpcServer.registry(zkRegistry);
            }
            registerRpcServices(rpcServer);
        }
        
        // 初始化 RPC 客户端（有 ZK 地址就创建，用于调用其他模块的服务）
        if (zkRegistry != null) {
            RpcClientConfig clientConfig = config.getRpcClientConfig();
            if (clientConfig == null) {
                clientConfig = new RpcClientConfig();
            }
            this.rpcProxyManager = new RpcProxyManager()
                    .discovery(zkRegistry)
                    .clientConfig(clientConfig)
                    .init();
            log.info("[{}] RPC client initialized", name());
        }
        
        // 初始化 Redis（以 module-serverId 为实例名，相同地址复用）
        String redisAddr = config.getRedisAddress();
        if (redisAddr != null && !redisAddr.isEmpty()) {
            String redisName = name() + "-" + config.getServerId();
            if (!RedisManager.hasInstance(redisName)) {
                RedisManager.register(redisName, redisAddr);
            }
        }
        
        // 初始化数据库
        String jdbcUrl = config.getJdbcUrl();
        if (jdbcUrl != null && !jdbcUrl.isEmpty()) {
            DbConfig dbConfig = config.getDbConfig();
            if (dbConfig == null) {
                dbConfig = new DbConfig()
                        .jdbcUrl(jdbcUrl)
                        .username(config.getJdbcUser())
                        .password(config.getJdbcPassword());
            }
            this.dbManager = new DbManager(dbConfig);
            log.info("[{}] Database initialized: {}", name(), jdbcUrl.replaceAll("\\?.*", ""));
        }
        
        // 初始化 Web 服务
        int webPort = webPort();
        if (webPort > 0) {
            this.webServer = new WebServer(webPort);
            // 注册 Web 路由
            registerWebRoutes(webServer);
        }
        
        // 子类自定义初始化
        doInit();
        
        log.info("[{}] Module initialized", name());
    }
    
    @Override
    public void start() throws Exception {
        if (running.get()) {
            log.warn("[{}] Module is already running", name());
            return;
        }
        
        log.info("[{}] Starting module...", name());
        
        // 启动 RPC 服务
        if (rpcServer != null) {
            rpcServer.start();
            log.info("[{}] RPC server started on port {}", name(), rpcPort());
        }
        
        // 启动 Web 服务
        if (webServer != null) {
            webServer.start();
            log.info("[{}] Web server started on port {}", name(), webPort());
        }
        
        // 子类自定义启动
        doStart();
        
        running.set(true);
        log.info("[{}] Module started successfully", name());
    }
    
    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        
        log.info("[{}] Stopping module...", name());
        
        // 子类自定义停止
        doStop();
        
        // 关闭数据库（等待异步落地完成）
        if (dbManager != null) {
            dbManager.shutdown();
        }
        
        // 停止 Web 服务
        if (webServer != null) {
            webServer.stop();
        }
        
        // 关闭 RPC 客户端
        if (rpcProxyManager != null) {
            rpcProxyManager.shutdown();
        }
        
        // 停止 RPC 服务端
        if (rpcServer != null) {
            rpcServer.shutdown();
        }
        
        // 关闭 ZK 注册中心
        if (zkRegistry != null) {
            zkRegistry.shutdown();
        }
        
        log.info("[{}] Module stopped", name());
    }
    
    @Override
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * 注册 RPC 服务
     * 子类重写此方法注册自己的 RPC 服务实现
     */
    protected void registerRpcServices(RpcServer server) {
        // 默认不注册任何服务
    }
    
    /**
     * 注册 Web 路由
     * 子类重写此方法注册 GM 后台接口
     */
    protected void registerWebRoutes(WebServer server) {
        // 默认不注册任何路由
    }
    
    /**
     * 子类自定义初始化
     */
    protected void doInit() {
        // 默认空实现
    }
    
    /**
     * 子类自定义启动
     */
    protected void doStart() throws Exception {
        // 默认空实现
    }
    
    /**
     * 子类自定义停止
     */
    protected void doStop() {
        // 默认空实现
    }
    
    /**
     * 从 ModuleConfig 构建 RpcServerConfig
     * 如果 YAML 中配置了完整的 rpcServerConfig 则直接使用，否则按兼容模式从散字段构建
     */
    private RpcServerConfig buildRpcServerConfig(ModuleConfig moduleConfig) {
        if (moduleConfig.getRpcServerConfig() != null) {
            return moduleConfig.getRpcServerConfig();
        }
        RpcServerConfig rpcConfig = new RpcServerConfig();
        if (moduleConfig.getRpcBacklog() > 0) {
            rpcConfig.backlog(moduleConfig.getRpcBacklog());
        }
        if (moduleConfig.getRpcReaderIdleTimeSeconds() > 0) {
            rpcConfig.readerIdleTimeSeconds(moduleConfig.getRpcReaderIdleTimeSeconds());
        }
        if (moduleConfig.getRpcSendBufferSize() > 0) {
            rpcConfig.sendBufferSize(moduleConfig.getRpcSendBufferSize());
        }
        if (moduleConfig.getRpcReceiveBufferSize() > 0) {
            rpcConfig.receiveBufferSize(moduleConfig.getRpcReceiveBufferSize());
        }
        return rpcConfig;
    }
    
    /**
     * 获取模块配置
     */
    protected ModuleConfig getConfig() {
        return config;
    }
    
    /**
     * 获取 RPC 服务器
     */
    protected RpcServer getRpcServer() {
        return rpcServer;
    }
    
    /**
     * 获取 RPC 代理管理器（用于调用其他模块的服务）
     *
     * <pre>
     * IWorldService worldService = getRpcProxy().get(IWorldService.class);
     * IGameService game2 = getRpcProxy().get(IGameService.class, 2);
     * </pre>
     */
    protected RpcProxyManager getRpcProxy() {
        return rpcProxyManager;
    }
    
    /**
     * 获取 Web 服务器
     */
    protected WebServer getWebServer() {
        return webServer;
    }
    
    /**
     * 获取数据库管理器
     */
    protected DbManager getDb() {
        return dbManager;
    }
    
    /**
     * 获取全局 Redis（infrastructure.redis 配置的）
     */
    protected RedisManager getGlobalRedis() {
        return RedisManager.hasInstance("global") ? RedisManager.of("global") : null;
    }
    
    /**
     * 获取当前实例独立的 Redis（实例级 redis 配置的）
     */
    protected RedisManager getRedis() {
        String redisName = name() + "-" + config.getServerId();
        return RedisManager.hasInstance(redisName) ? RedisManager.of(redisName) : null;
    }
}
