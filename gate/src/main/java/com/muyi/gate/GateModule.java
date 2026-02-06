package com.muyi.gate;

import com.muyi.core.module.AbstractGameModule;
import com.muyi.core.web.WebServer;
import com.muyi.gate.migrate.ServerMigrator;
import com.muyi.gate.router.MessageRouter;
import com.muyi.gate.service.GateServiceImpl;
import com.muyi.gate.session.SessionManager;
import com.muyi.rpc.server.RpcServer;

/**
 * Gate 模块
 * 网关服务，纯粹的连接转发层，不含业务逻辑
 * 
 * 职责：
 * 1. 客户端 TCP 长连接管理
 * 2. 消息路由转发（根据协议号分发到 Game/World/Alliance）
 * 3. 无缝跨服/迁服（只切换路由目标，不断开连接）
 * 
 * 不提供 Web/GM 接口，所有管理操作通过 RPC 调用
 *
 * @author muyi
 */
public class GateModule extends AbstractGameModule {
    
    private SessionManager sessionManager;
    private MessageRouter messageRouter;
    private ServerMigrator serverMigrator;
    private GateServiceImpl gateService;
    
    @Override
    public String name() {
        return "gate";
    }
    
    @Override
    public String description() {
        return "网关服务 - 连接管理、消息路由、无缝跨服";
    }
    
    @Override
    public int priority() {
        return 90; // 登录之后启动
    }
    
    @Override
    public int rpcPort() {
        return config != null ? config.getRpcPort() : 10002;
    }
    
    @Override
    public int webPort() {
        // Gate 不提供 Web 接口，返回 0 表示不启动
        return 0;
    }
    
    @Override
    protected void doInit() {
        // 初始化会话管理器
        sessionManager = new SessionManager();
        
        // 初始化消息路由器
        messageRouter = new MessageRouter();
        
        // 初始化迁服器
        serverMigrator = new ServerMigrator(sessionManager);
        
        // 初始化 RPC 服务
        gateService = new GateServiceImpl(sessionManager, serverMigrator);
        
        log.info("Gate module initialized with {} route rules", messageRouter.getRuleCount());
    }
    
    @Override
    protected void registerRpcServices(RpcServer server) {
        server.registerService(gateService);
    }
    
    @Override
    protected void registerWebRoutes(WebServer server) {
        // Gate 不提供 Web 接口，所有管理操作通过 RPC
    }
    
    // ==================== Getter for external access ====================
    
    public SessionManager getSessionManager() {
        return sessionManager;
    }
    
    public MessageRouter getMessageRouter() {
        return messageRouter;
    }
    
    public ServerMigrator getServerMigrator() {
        return serverMigrator;
    }
}
