package com.muyi.world;

import com.muyi.core.module.AbstractGameModule;
import com.muyi.core.web.WebServer;
import com.muyi.rpc.server.RpcServer;
import com.muyi.world.controller.WorldGmController;

/**
 * World 模块骨架
 * 
 * 负责世界地图、活动、排行榜等全局数据管理，具体业务由各项目实现
 * 
 * 扩展方式：
 * 1. 继承此类，重写 doInit() 初始化业务组件
 * 2. 重写 registerRpcServices() 注册 RPC 服务
 * 3. 重写 registerWebRoutes() 注册 GM 接口
 *
 * @author muyi
 */
public class WorldModule extends AbstractGameModule {
    
    protected WorldGmController gmController;
    
    @Override
    public String name() {
        return "world";
    }
    
    @Override
    public String description() {
        return "世界服务 - 地图/活动/排行榜管理";
    }
    
    @Override
    public int priority() {
        return 70;
    }
    
    @Override
    public int rpcPort() {
        return config != null ? config.getRpcPort() : 10004;
    }
    
    @Override
    public int webPort() {
        return config != null ? config.getWebPort() : 18004;
    }
    
    @Override
    protected void doInit() {
        gmController = createGmController();
        log.info("World module initialized, serverId={}", 
                config != null ? config.getServerId() : -1);
    }
    
    /**
     * 创建 GM 控制器，子类可重写以扩展
     */
    protected WorldGmController createGmController() {
        return new WorldGmController();
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
