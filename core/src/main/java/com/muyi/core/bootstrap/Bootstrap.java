package com.muyi.core.bootstrap;

import com.muyi.common.redis.RedisManager;
import com.muyi.core.config.ModuleConfig;
import com.muyi.core.config.ServerConfig;
import com.muyi.core.config.ServerConfig.InstanceConfig;
import com.muyi.core.module.GameModule;
import com.muyi.rpc.transport.SharedEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 服务器启动器
 * 启动配置中定义的所有实例
 *
 * @author muyi
 */
public class Bootstrap {
    
    private static final Logger log = LoggerFactory.getLogger(Bootstrap.class);
    
    private final ServerConfig serverConfig;
    private final ModuleRegistry registry;
    private final List<StartedModule> startedModules = new ArrayList<>();
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    public Bootstrap(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
        this.registry = ModuleRegistry.getInstance();
    }
    
    /**
     * 启动服务器
     */
    public void start() throws Exception {
        if (!running.compareAndSet(false, true)) {
            log.warn("Server is already running");
            return;
        }
        
        log.info("========================================");
        log.info("  SLG Server Starting...");
        log.info("  Instances: {}", serverConfig.getInstances().size());
        log.info("========================================");
        
        // 通过 SPI 发现模块
        registry.discoverModules();
        
        // 初始化共享 EventLoopGroup（所有 RPC Server 共用）
        SharedEventLoopGroup.init(serverConfig.getRpcWorkerThreads());
        
        // 应用 RPC 全局配置
        if (serverConfig.getRpcMaxFrameLength() > 0) {
            com.muyi.rpc.protocol.RpcProtocol.setMaxFrameLength(serverConfig.getRpcMaxFrameLength());
        }
        if (serverConfig.getRpcCompressThreshold() > 0) {
            com.muyi.rpc.compress.CompressorFactory.setCompressThreshold(serverConfig.getRpcCompressThreshold());
        }
        
        // 应用 HTTP 全局配置
        if (serverConfig.getHttpConnectTimeout() > 0 || serverConfig.getHttpReadTimeout() > 0 
                || serverConfig.getHttpWriteTimeout() > 0) {
            com.muyi.common.util.net.HttpUtils.configure(
                    serverConfig.getHttpConnectTimeout() > 0 ? serverConfig.getHttpConnectTimeout() : 10,
                    serverConfig.getHttpReadTimeout() > 0 ? serverConfig.getHttpReadTimeout() : 30,
                    serverConfig.getHttpWriteTimeout() > 0 ? serverConfig.getHttpWriteTimeout() : 30);
        }
        
        // 应用 Redis 连接池全局配置
        RedisManager.configure(
                serverConfig.getRedisMaxTotal(),
                serverConfig.getRedisMaxIdle(),
                serverConfig.getRedisMinIdle(),
                serverConfig.getRedisMaxWaitSeconds(),
                serverConfig.getRedisConnectTimeout());
        
        // 注册全局 Redis
        String globalRedis = serverConfig.getRedisAddress();
        if (globalRedis != null && !globalRedis.isEmpty()) {
            RedisManager.register("global", globalRedis);
        }
        
        // 按配置顺序启动所有实例
        for (InstanceConfig instance : serverConfig.getInstances()) {
            startInstance(instance);
        }
        
        // 注册 Shutdown Hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown hook triggered");
            shutdown();
        }, "shutdown-hook"));
        
        log.info("========================================");
        log.info("  SLG Server Started Successfully!");
        log.info("  Started: {}", startedModules.stream().map(m -> m.instanceId).toList());
        log.info("========================================");
    }
    
    /**
     * 启动单个实例
     */
    private void startInstance(InstanceConfig instance) throws Exception {
        GameModule module = registry.get(instance.module);
        if (module == null) {
            log.warn("Module not found: {}", instance.module);
            return;
        }
        
        // 为每个实例创建新的模块实例
        GameModule moduleInstance = module.getClass().getDeclaredConstructor().newInstance();
        ModuleConfig config = serverConfig.getModuleConfig(instance);
        
        String instanceId = instance.getInstanceId();
        
        log.info("Initializing {}...", instanceId);
        moduleInstance.init(config);
        
        log.info("Starting {}...", instanceId);
        moduleInstance.start();
        
        startedModules.add(new StartedModule(instanceId, moduleInstance));
        log.info("{} started", instanceId);
    }
    
    /**
     * 阻塞等待关闭
     */
    public void awaitShutdown() throws InterruptedException {
        shutdownLatch.await();
    }
    
    /**
     * 关闭服务器
     */
    public void shutdown() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        doShutdown();
    }
    
    /**
     * 执行关闭逻辑
     */
    private void doShutdown() {
        log.info("========================================");
        log.info("  SLG Server Shutting down...");
        log.info("========================================");
        
        // 逆序停止模块
        for (int i = startedModules.size() - 1; i >= 0; i--) {
            StartedModule started = startedModules.get(i);
            try {
                log.info("Stopping {}...", started.instanceId);
                started.module.stop();
            } catch (Exception e) {
                log.error("Error stopping {}", started.instanceId, e);
            }
        }
        
        startedModules.clear();
        
        // 关闭共享 EventLoopGroup（兜底，确保所有 RpcServer 关闭后线程组被释放）
        SharedEventLoopGroup.shutdownGlobal();
        
        // 关闭 Redis 连接池
        RedisManager.shutdownAll();
        
        log.info("========================================");
        log.info("  SLG Server Stopped");
        log.info("========================================");
        
        shutdownLatch.countDown();
    }
    
    /**
     * 是否运行中
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * 获取服务器配置
     */
    public ServerConfig getServerConfig() {
        return serverConfig;
    }
    
    /**
     * 获取模块注册中心
     */
    public ModuleRegistry getRegistry() {
        return registry;
    }
    
    /**
     * 已启动的模块信息
     */
    private static class StartedModule {
        final String instanceId;
        final GameModule module;
        
        StartedModule(String instanceId, GameModule module) {
            this.instanceId = instanceId;
            this.module = module;
        }
    }
    
    // ==================== 静态入口 ====================
    
    /**
     * 命令行启动入口
     * 
     * 用法:
     *   java -jar slgserver.jar
     *   java -jar slgserver.jar --config=xxx.yaml
     */
    static void main(String[] args) {
        try {
            String configPath = "serverconfig/server.yaml";
            
            for (String arg : args) {
                if (arg.startsWith("--config=")) {
                    configPath = arg.substring("--config=".length());
                }
            }
            
            // 加载配置
            ServerConfig config = ServerConfig.load(configPath);
            
            // 启动
            Bootstrap bootstrap = new Bootstrap(config);
            bootstrap.start();
            bootstrap.awaitShutdown();
            
        } catch (Exception e) {
            log.error("Server startup failed", e);
            System.exit(1);
        }
    }
}
