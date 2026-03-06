package com.muyi.core.module;

import com.muyi.common.redis.RedisManager;
import com.muyi.common.util.log.GameLog;
import com.muyi.core.config.ModuleConfig;
import com.muyi.core.log.ModuleRegistry;
import com.muyi.core.scheduler.GameScheduler;
import com.muyi.core.thread.ThreadPoolManager;
import com.muyi.core.web.GroovyHandler;
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

import java.util.concurrent.TimeUnit;
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
    
    /** 模块级线程池管理器（统一管理和监控所有线程池） */
    private ThreadPoolManager poolManager;
    
    /** 模块级定时任务调度器（可选，需在 doInit 中调用 initScheduler 初始化） */
    private GameScheduler scheduler;
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    @Override
    public void init(ModuleConfig config) {
        this.config = config;
        
        // 注册包名 → 模块映射（Logback Converter 自动根据 Logger 名称识别模块）
        String pkg = getClass().getPackageName();
        ModuleRegistry.register(pkg, name(), String.valueOf(config.getServerId()));
        // 设置 ITL（用于框架共享代码的 fallback，如 rpc/db 线程）
        GameLog.init(name(), config.getServerId());
        
        log.info("Initializing module...");
        
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
            rpcServer.logTag(name());
            rpcServer.taskDecorator(task -> () -> ModuleContext.runWith(this, task));
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
            log.info("RPC client initialized");
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
            log.info("Database initialized: {}", jdbcUrl.replaceAll("\\?.*", ""));
        }
        
        // 初始化线程池管理器（传入 this，所有线程池任务执行前自动设置模块上下文）
        this.poolManager = new ThreadPoolManager(name(), this);
        
        // 初始化 Web 服务
        int webPort = webPort();
        if (webPort > 0) {
            this.webServer = new WebServer(webPort).moduleContext(name(), String.valueOf(config.getServerId()));
            registerWebRoutes(webServer);
            
            if (config.isGroovyEnabled()) {
                new GroovyHandler(this).register(webServer);
            }
        }
        
        // 子类自定义初始化
        doInit();
        
        log.info("Module initialized");
    }
    
    @Override
    public void start() throws Exception {
        if (running.get()) {
            log.warn("Module is already running");
            return;
        }
        
        log.info("Starting module...");
        
        // 启动 RPC 服务
        if (rpcServer != null) {
            rpcServer.start();
            log.info("RPC server started on port {}", rpcPort());
        }
        
        // 启动 Web 服务
        if (webServer != null) {
            webServer.start();
            log.info("Web server started on port {}", webPort());
        }
        
        // 子类自定义启动
        doStart();
        
        running.set(true);
        log.info("Module started successfully");
    }
    
    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        
        log.info("Stopping module...");
        
        // 子类自定义停止
        doStop();
        
        // 关闭定时任务调度器
        if (scheduler != null) {
            scheduler.shutdown();
        }
        
        // 关闭所有线程池
        if (poolManager != null) {
            poolManager.shutdown();
        }
        
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
        
        log.info("Module stopped");
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
     * 获取服务器 ID
     */
    public int getServerId() {
        return config != null ? config.getServerId() : -1;
    }

    /**
     * 获取模块配置
     */
    public ModuleConfig getConfig() {
        return config;
    }
    
    /**
     * 获取 RPC 服务器
     */
    public RpcServer getRpcServer() {
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
    public RpcProxyManager getRpcProxy() {
        return rpcProxyManager;
    }
    
    /**
     * 获取 ZooKeeper 注册中心（用于添加自定义 metadata 等）
     */
    public ZookeeperServiceRegistry getZkRegistry() {
        return zkRegistry;
    }
    
    /**
     * 获取 Web 服务器
     */
    public WebServer getWebServer() {
        return webServer;
    }
    
    /**
     * 获取数据库管理器
     */
    public DbManager getDb() {
        return dbManager;
    }
    
    /**
     * 获取全局 Redis（infrastructure.redis 配置的）
     */
    public RedisManager getGlobalRedis() {
        return RedisManager.hasInstance("global") ? RedisManager.of("global") : null;
    }
    
    /**
     * 获取当前实例独立的 Redis（实例级 redis 配置的）
     */
    public RedisManager getRedis() {
        String redisName = name() + "-" + config.getServerId();
        return RedisManager.hasInstance(redisName) ? RedisManager.of(redisName) : null;
    }
    
    // ==================== 定时任务调度器 ====================
    /**
     * 初始化定时任务调度器（自定义调度线程数）
     *
     * @param threads 调度线程数（通常 1 即可，大量任务可适当增加）
     */
    protected void initScheduler(int threads) {
        if (this.scheduler != null) {
            throw new IllegalStateException("Scheduler already initialized");
        }
        this.scheduler = new GameScheduler(poolManager.newScheduledPool("scheduler", threads));
    }
    
    /**
     * 获取定时任务调度器
     *
     * @throws IllegalStateException 如果尚未调用 initScheduler
     */
    public GameScheduler getScheduler() {
        if (scheduler == null) {
            throw new IllegalStateException("Scheduler not initialized, call initScheduler() in doInit() first");
        }
        return scheduler;
    }
    
    // ==================== 线程池 ====================
    
    /**
     * 获取线程池管理器
     * <p>
     * 通过管理器创建和获取业务线程池：
     * <pre>{@code
     * // 在 doInit() 中创建
     * getPoolManager().newFixedPool("battle", 4);
     * getPoolManager().newVirtualPool("io");
     *
     * // 在业务代码中使用
     * ExecutorService battle = getPoolManager().get("battle");
     * battle.execute(() -> processBattle());
     * }</pre>
     */
    public ThreadPoolManager getPoolManager() {
        return poolManager;
    }
}
