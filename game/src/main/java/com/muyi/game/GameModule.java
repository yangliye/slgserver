package com.muyi.game;

import com.muyi.core.module.AbstractGameModule;
import com.muyi.core.web.WebServer;
import com.muyi.game.controller.GameGmController;
import com.muyi.rpc.server.RpcServer;

/**
 * Game 模块骨架
 * 
 * 负责玩家个人数据管理，具体业务由各项目实现
 * 
 * 扩展方式：
 * 1. 继承此类，重写 doInit() 初始化业务组件
 * 2. 重写 registerRpcServices() 注册 RPC 服务
 * 3. 重写 registerWebRoutes() 注册 GM 接口
 *
 * @author muyi
 */
public class GameModule extends AbstractGameModule {
    
    protected GameGmController gmController;
    
    @Override
    public String name() {
        return "game";
    }
    
    @Override
    public String description() {
        return "游戏核心服务 - 玩家数据管理";
    }
    
    @Override
    public int priority() {
        return 80;
    }
    
    @Override
    public int rpcPort() {
        return config != null ? config.getRpcPort() : 10003;
    }
    
    @Override
    public int webPort() {
        return config != null ? config.getWebPort() : 18003;
    }
    
    @Override
    protected void doInit() {
        gmController = createGmController();
        log.info("Game module initialized, serverId={}", 
                config != null ? config.getServerId() : -1);
    }
    
    /**
     * 创建 GM 控制器，子类可重写以扩展
     */
    protected GameGmController createGmController() {
        return new GameGmController();
    }
    
    @Override
    protected void registerRpcServices(RpcServer server) {
        // 由子类注册 RPC 服务
    }
    
    @Override
    protected void registerWebRoutes(WebServer server) {
        server.registerController(gmController);
    }
}
