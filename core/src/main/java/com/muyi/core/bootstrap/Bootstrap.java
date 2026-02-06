package com.muyi.core.bootstrap;

import com.muyi.core.config.ModuleConfig;
import com.muyi.core.config.ServerConfig;
import com.muyi.core.config.ServerConfig.InstanceConfig;
import com.muyi.core.module.GameModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 服务器启动器
 * 根据实例配置启动指定的模块
 *
 * @author muyi
 */
public class Bootstrap {
    
    private static final Logger log = LoggerFactory.getLogger(Bootstrap.class);
    
    private final ServerConfig serverConfig;
    private final InstanceConfig instanceConfig;
    private final ModuleRegistry registry;
    private final List<GameModule> startedModules = new ArrayList<>();
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    /**
     * 创建启动器
     * 
     * @param serverConfig 服务器配置
     * @param instanceName 要启动的实例名称
     */
    public Bootstrap(ServerConfig serverConfig, String instanceName) {
        this.serverConfig = serverConfig;
        this.instanceConfig = serverConfig.getInstance(instanceName);
        this.registry = ModuleRegistry.getInstance();
        
        if (this.instanceConfig == null) {
            throw new IllegalArgumentException("Instance not found: " + instanceName);
        }
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
        log.info("  Instance: {}", instanceConfig.name);
        log.info("  Server ID: {}", instanceConfig.serverId);
        log.info("  Modules: {}", instanceConfig.modules);
        log.info("========================================");
        
        // 通过 SPI 发现模块
        registry.discoverModules();
        
        // 获取要启动的模块列表
        List<GameModule> modulesToStart = getModulesToStart();
        if (modulesToStart.isEmpty()) {
            log.warn("No modules to start");
            running.set(false);
            return;
        }
        
        // 按优先级排序
        modulesToStart.sort(Comparator.comparingInt(GameModule::priority));
        
        log.info("Modules to start (ordered): {}", 
                modulesToStart.stream().map(m -> m.name() + "(" + m.priority() + ")").toList());
        
        // 初始化并启动模块
        for (GameModule module : modulesToStart) {
            try {
                ModuleConfig moduleConfig = serverConfig.getModuleConfig(instanceConfig, module.name());
                
                log.info("Initializing module: {}", module.name());
                module.init(moduleConfig);
                
                log.info("Starting module: {}", module.name());
                module.start();
                
                startedModules.add(module);
            } catch (Exception e) {
                log.error("Failed to start module: {}", module.name(), e);
                // 启动失败，重置状态并停止已启动的模块
                running.set(false);
                doShutdown();
                throw e;
            }
        }
        
        // 注册 Shutdown Hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown hook triggered");
            shutdown();
        }, "shutdown-hook"));
        
        log.info("========================================");
        log.info("  SLG Server Started Successfully!");
        log.info("  Instance: {}", instanceConfig.name);
        log.info("  Started modules: {}", startedModules.stream().map(GameModule::name).toList());
        log.info("========================================");
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
     * 执行关闭逻辑（内部方法）
     */
    private void doShutdown() {
        log.info("========================================");
        log.info("  SLG Server Shutting down...");
        log.info("  Instance: {}", instanceConfig.name);
        log.info("========================================");
        
        // 逆序停止模块
        for (int i = startedModules.size() - 1; i >= 0; i--) {
            GameModule module = startedModules.get(i);
            try {
                log.info("Stopping module: {}", module.name());
                module.stop();
            } catch (Exception e) {
                log.error("Error stopping module: {}", module.name(), e);
            }
        }
        
        startedModules.clear();
        
        log.info("========================================");
        log.info("  SLG Server Stopped");
        log.info("========================================");
        
        shutdownLatch.countDown();
    }
    
    /**
     * 获取要启动的模块列表
     */
    private List<GameModule> getModulesToStart() {
        List<GameModule> result = new ArrayList<>();
        
        for (String moduleName : instanceConfig.modules) {
            GameModule module = registry.get(moduleName);
            if (module != null) {
                result.add(module);
            } else {
                log.warn("Module not found: {}", moduleName);
            }
        }
        
        return result;
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
     * 获取实例配置
     */
    public InstanceConfig getInstanceConfig() {
        return instanceConfig;
    }
    
    /**
     * 获取模块注册中心
     */
    public ModuleRegistry getRegistry() {
        return registry;
    }
    
    /**
     * 获取已启动的模块
     */
    public List<GameModule> getStartedModules() {
        return new ArrayList<>(startedModules);
    }
    
    // ==================== 静态入口 ====================
    
    /**
     * 命令行启动入口
     * 
     * 用法:
     *   java -jar slgserver.jar --instance=game-1
     *   java -jar slgserver.jar --config=xxx.yaml --instance=login-1
     */
    public static void main(String[] args) {
        try {
            String configPath = null;
            String instanceName = null;
            
            for (String arg : args) {
                if (arg.startsWith("--config=")) {
                    configPath = arg.substring("--config=".length());
                } else if (arg.startsWith("--instance=")) {
                    instanceName = arg.substring("--instance=".length());
                }
            }
            
            if (instanceName == null) {
                System.err.println("Usage: java -jar slgserver.jar --instance=<name> [--config=<path>]");
                System.err.println("  --instance=<name>  Instance name to start (required)");
                System.err.println("  --config=<path>    Config file path (default: serverconfig/server.yaml)");
                System.exit(1);
                return;
            }
            
            // 加载配置
            ServerConfig config;
            if (configPath != null) {
                config = ServerConfig.load(configPath);
            } else {
                config = ServerConfig.load("serverconfig/server.yaml");
            }
            
            // 启动
            Bootstrap bootstrap = new Bootstrap(config, instanceName);
            bootstrap.start();
            bootstrap.awaitShutdown();
            
        } catch (Exception e) {
            log.error("Server startup failed", e);
            System.exit(1);
        }
    }
}
