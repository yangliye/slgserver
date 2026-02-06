package com.muyi.login.controller;

import com.muyi.core.web.annotation.GmApi;
import com.muyi.core.web.annotation.GmController;
import com.muyi.core.web.annotation.HttpMethod;
import com.muyi.core.web.annotation.Param;
import com.muyi.shared.dto.ClientParams;

import java.util.Map;

/**
 * Login 模块 API 控制器
 * 
 * 玩家面向的 HTTP 接口：登录、注册、服务器列表等
 * 具体业务由各项目扩展实现
 *
 * @author muyi
 */
@GmController("/api")
public class LoginApiController {
    
    /**
     * 账号注册
     * 
     * @param clientParams 客户端参数（包含 account, password, deviceId 等）
     * @return 注册结果
     */
    @GmApi(path = "/register", method = HttpMethod.POST, description = "账号注册")
    public Map<String, Object> register(ClientParams clientParams) {
        // 由子类实现具体逻辑
        throw new UnsupportedOperationException("待实现");
    }
    
    /**
     * 账号登录
     * 
     * @param clientParams 客户端参数（包含 account, password, deviceId, platform 等）
     * @return 登录结果（含 token）
     */
    @GmApi(path = "/login", method = HttpMethod.POST, description = "账号登录")
    public Map<String, Object> login(ClientParams clientParams) {
        // 由子类实现具体逻辑
        throw new UnsupportedOperationException("待实现");
    }
    
    /**
     * 获取服务器列表
     * 
     * @param token 登录令牌
     * @return 服务器列表
     */
    @GmApi(path = "/server-list", description = "获取服务器列表")
    public Map<String, Object> serverList(@Param("token") String token) {
        // 由子类实现具体逻辑
        throw new UnsupportedOperationException("待实现");
    }
    
    /**
     * 选择服务器
     * 
     * @param token 登录令牌
     * @param serverId 服务器ID
     * @return 选服结果（含 Gate 地址）
     */
    @GmApi(path = "/select-server", method = HttpMethod.POST, description = "选择服务器")
    public Map<String, Object> selectServer(
            @Param("token") String token,
            @Param("serverId") int serverId) {
        // 由子类实现具体逻辑
        throw new UnsupportedOperationException("待实现");
    }
    
    /**
     * 验证 Token
     * 
     * @param token 登录令牌
     * @return 验证结果
     */
    @GmApi(path = "/verify-token", description = "验证Token")
    public Map<String, Object> verifyToken(@Param("token") String token) {
        // 由子类实现具体逻辑
        throw new UnsupportedOperationException("待实现");
    }
}
