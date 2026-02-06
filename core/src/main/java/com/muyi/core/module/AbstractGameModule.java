package com.muyi.core.module;

import com.muyi.core.config.ModuleConfig;
import com.muyi.core.web.WebServer;
import com.muyi.rpc.server.RpcServer;
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
    protected WebServer webServer;
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    @Override
    public void init(ModuleConfig config) {
        this.config = config;
        log.info("[{}] Initializing module...", name());
        
        // 初始化 RPC 服务
        int rpcPort = rpcPort();
        if (rpcPort > 0) {
            this.rpcServer = new RpcServer(rpcPort);
            rpcServer.serverId(config.getServerId());
            if (config.getHost() != null) {
                rpcServer.host(config.getHost());
            }
            // 注册 RPC 服务
            registerRpcServices(rpcServer);
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
        
        // 停止 Web 服务
        if (webServer != null) {
            webServer.stop();
        }
        
        // 停止 RPC 服务
        if (rpcServer != null) {
            rpcServer.shutdown();
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
     * 获取 Web 服务器
     */
    protected WebServer getWebServer() {
        return webServer;
    }
}
