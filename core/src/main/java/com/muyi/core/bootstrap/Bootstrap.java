package com.muyi.core.bootstrap;

import com.muyi.core.config.ModuleConfig;
import com.muyi.core.config.ServerConfig;
import com.muyi.core.module.GameModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 服务器启动器
 * 统一管理所有模块的生命周期
 *
 * @author muyi
 */
public class Bootstrap {
    
    private static final Logger log = LoggerFactory.getLogger(Bootstrap.class);
    
    private final ServerConfig serverConfig;
    private final ModuleRegistry registry;
    private final List<GameModule> startedModules = new ArrayList<>();
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);
    
    private volatile boolean running = false;
    
    public Bootstrap(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
        this.registry = ModuleRegistry.getInstance();
    }
    
    /**
     * 启动服务器
     */
    public void start() throws Exception {
        if (running) {
            log.warn("Server is already running");
            return;
        }
        
        log.info("========================================");
        log.info("  SLG Server Starting...");
        log.info("  Server ID: {}", serverConfig.getServerId());
        log.info("  Mode: {}", serverConfig.getMode());
        log.info("========================================");
        
        // 通过 SPI 发现模块
        registry.discoverModules();
        
        // 获取要启动的模块列表
        List<GameModule> modulesToStart = getModulesToStart();
        if (modulesToStart.isEmpty()) {
            log.warn("No modules to start");
            return;
        }
        
        // 按优先级排序
        modulesToStart.sort(Comparator.comparingInt(GameModule::priority));
        
        log.info("Modules to start: {}", modulesToStart.stream().map(GameModule::name).toList());
        
        // 初始化并启动模块
        for (GameModule module : modulesToStart) {
            try {
                ModuleConfig moduleConfig = serverConfig.getModuleConfig(module.name());
                
                log.info("Initializing module: {}", module.name());
                module.init(moduleConfig);
                
                log.info("Starting module: {}", module.name());
                module.start();
                
                startedModules.add(module);
            } catch (Exception e) {
                log.error("Failed to start module: {}", module.name(), e);
                // 启动失败，停止已启动的模块
                shutdown();
                throw e;
            }
        }
        
        running = true;
        
        // 注册 Shutdown Hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown hook triggered");
            shutdown();
        }, "shutdown-hook"));
        
        log.info("========================================");
        log.info("  SLG Server Started Successfully!");
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
        if (!running) {
            return;
        }
        running = false;
        
        log.info("========================================");
        log.info("  SLG Server Shutting down...");
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
        
        if (serverConfig.isAllModules()) {
            // 启动所有已注册的模块
            result.addAll(registry.getAll());
        } else {
            // 只启动配置中指定的模块
            for (String moduleName : serverConfig.getModules()) {
                GameModule module = registry.get(moduleName);
                if (module != null) {
                    result.add(module);
                } else {
                    log.warn("Module not found: {}", moduleName);
                }
            }
        }
        
        return result;
    }
    
    /**
     * 是否运行中
     */
    public boolean isRunning() {
        return running;
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
     *   java -jar slgserver.jar                    # 使用默认配置
     *   java -jar slgserver.jar --config=xxx.yaml  # 使用指定配置
     *   java -jar slgserver.jar --module=game      # 只启动指定模块
     */
    static void main(String[] args) {
        try {
            ServerConfig config = parseArgs(args);
            
            Bootstrap bootstrap = new Bootstrap(config);
            bootstrap.start();
            bootstrap.awaitShutdown();
            
        } catch (Exception e) {
            log.error("Server startup failed", e);
            System.exit(1);
        }
    }
    
    /**
     * 解析命令行参数
     */
    private static ServerConfig parseArgs(String[] args) throws Exception {
        String configPath = null;
        String module = null;
        
        for (String arg : args) {
            if (arg.startsWith("--config=")) {
                configPath = arg.substring("--config=".length());
            } else if (arg.startsWith("--module=")) {
                module = arg.substring("--module=".length());
            }
        }
        
        ServerConfig config;
        if (configPath != null) {
            config = ServerConfig.load(configPath);
        } else {
            // 尝试加载默认配置
            try {
                config = ServerConfig.loadFromClasspath("server.yaml");
            } catch (Exception e) {
                // 使用默认配置
                config = new ServerConfig();
            }
        }
        
        // 命令行指定单模块启动
        if (module != null) {
            config.setMode("single");
            config.setModules(List.of(module));
        }
        
        return config;
    }
}
