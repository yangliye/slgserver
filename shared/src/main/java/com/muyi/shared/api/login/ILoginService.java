package com.muyi.shared.api.login;

/**
 * Login 模块 RPC 服务接口
 * <p>
 * Token 验证已改为 Redis 直连（{@link com.muyi.shared.token.TokenStore}），
 * 此接口预留给 Login 模块其他 RPC 能力扩展。
 *
 * @author muyi
 */
public interface ILoginService {
}
