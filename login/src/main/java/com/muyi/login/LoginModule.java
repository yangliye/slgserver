package com.muyi.login;

import com.muyi.core.module.AbstractGameModule;
import com.muyi.core.web.WebServer;
import com.muyi.login.controller.LoginApiController;
import com.muyi.login.controller.LoginGmController;
import com.muyi.login.service.LoginServiceImpl;
import com.muyi.login.service.TokenService;
import com.muyi.rpc.server.RpcServer;

/**
 * Login 模块骨架
 * 
 * 负责玩家登录、注册、Token管理等，具体业务由各项目实现
 * 
 * 扩展方式：
 * 1. 继承此类，重写 doInit() 初始化业务组件
 * 2. 重写 registerRpcServices() 注册 RPC 服务
 * 3. 重写 registerWebRoutes() 注册 HTTP API / GM 接口
 * 4. 重写 createApiController() / createGmController() 扩展控制器
 *
 * @author muyi
 */
public class LoginModule extends AbstractGameModule {
    
    protected TokenService tokenService;
    protected LoginServiceImpl loginService;
    protected LoginApiController apiController;
    protected LoginGmController gmController;
    
    @Override
    public String name() {
        return "login";
    }
    
    @Override
    public String description() {
        return "登录服务 - 账号/Token管理";
    }
    
    @Override
    public int priority() {
        return 100;
    }
    
    @Override
    public int rpcPort() {
        return config != null ? config.getRpcPort() : 10001;
    }
    
    @Override
    public int webPort() {
        return config != null ? config.getWebPort() : 18001;
    }
    
    @Override
    protected void doInit() {
        tokenService = new TokenService();
        tokenService.setRedis(getGlobalRedis());
        
        loginService = new LoginServiceImpl();
        
        apiController = createApiController();
        apiController.setTokenService(tokenService);
        apiController.setRpcProxy(getRpcProxy());
        apiController.setGlobalRedis(getGlobalRedis());
        
        gmController = createGmController();
        log.info("Login module initialized");
    }
    
    /**
     * 创建 API 控制器（玩家接口），子类可重写以扩展
     */
    protected LoginApiController createApiController() {
        return new LoginApiController();
    }
    
    /**
     * 创建 GM 控制器（后台接口），子类可重写以扩展
     */
    protected LoginGmController createGmController() {
        return new LoginGmController();
    }
    
    @Override
    protected void registerRpcServices(RpcServer server) {
        server.registerService(loginService);
    }
    
    @Override
    protected void registerWebRoutes(WebServer server) {
        server.registerController(apiController);
        server.registerController(gmController);
    }
}
