package com.muyi.alliance;

import com.muyi.alliance.controller.AllianceGmController;
import com.muyi.core.module.AbstractGameModule;
import com.muyi.core.web.WebServer;
import com.muyi.rpc.server.RpcServer;

/**
 * Alliance 模块骨架
 * 
 * 负责联盟相关业务管理，具体业务由各项目实现
 * 
 * 扩展方式：
 * 1. 继承此类，重写 doInit() 初始化业务组件
 * 2. 重写 registerRpcServices() 注册 RPC 服务
 * 3. 重写 registerWebRoutes() 注册 GM 接口
 *
 * @author muyi
 */
public class AllianceModule extends AbstractGameModule {
    
    protected AllianceGmController gmController;
    
    @Override
    public String name() {
        return "alliance";
    }
    
    @Override
    public String description() {
        return "联盟服务 - 联盟管理";
    }
    
    @Override
    public int priority() {
        return 60;
    }
    
    @Override
    public int rpcPort() {
        return config != null ? config.getRpcPort() : 10005;
    }
    
    @Override
    public int webPort() {
        return config != null ? config.getWebPort() : 18005;
    }
    
    @Override
    protected void doInit() {
        gmController = createGmController();
        log.info("Alliance module initialized, serverId={}", 
                config != null ? config.getServerId() : -1);
    }
    
    /**
     * 创建 GM 控制器，子类可重写以扩展
     */
    protected AllianceGmController createGmController() {
        return new AllianceGmController();
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
