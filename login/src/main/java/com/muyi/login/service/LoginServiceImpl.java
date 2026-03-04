package com.muyi.login.service;

import com.muyi.rpc.annotation.RpcService;
import com.muyi.shared.api.login.ILoginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Login RPC 服务实现
 * <p>
 * Token 验证已改为 Redis 直连，此类预留给 Login 模块其他 RPC 能力。
 *
 * @author muyi
 */
@RpcService(ILoginService.class)
public class LoginServiceImpl implements ILoginService {

    private static final Logger log = LoggerFactory.getLogger(LoginServiceImpl.class);
}
