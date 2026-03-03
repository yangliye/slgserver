package com.muyi.login.controller;

import com.muyi.core.web.annotation.GmApi;
import com.muyi.core.web.annotation.GmController;
import com.muyi.core.web.annotation.HttpMethod;
import com.muyi.core.web.annotation.Param;
import com.muyi.common.redis.RedisManager;
import com.muyi.login.service.TokenService;
import com.muyi.rpc.client.RpcProxyManager;
import com.muyi.rpc.registry.ServiceInstance;
import com.muyi.shared.api.IGameService;
import com.muyi.shared.api.IGateService;
import com.muyi.shared.dto.ClientParams;
import com.muyi.shared.dto.TokenInfo;
import com.muyi.shared.token.TokenStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Login 模块 API 控制器
 * <p>
 * 玩家面向的 HTTP 接口：登录、注册、服务器列表等。
 * 通过 {@link RpcProxyManager} 从 ZooKeeper 动态发现 Gate/Game/World 服务实例。
 * 具体业务（账号系统、服务器分配策略）由各项目子类扩展。
 *
 * @author muyi
 */
@GmController("/api")
public class LoginApiController {

    private static final Logger log = LoggerFactory.getLogger(LoginApiController.class);

    private TokenService tokenService;
    private RpcProxyManager rpcProxy;
    private RedisManager globalRedis;

    public void setTokenService(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    public void setRpcProxy(RpcProxyManager rpcProxy) {
        this.rpcProxy = rpcProxy;
    }

    public void setGlobalRedis(RedisManager globalRedis) {
        this.globalRedis = globalRedis;
    }

    /**
     * 账号注册
     */
    @GmApi(path = "/register", method = HttpMethod.POST, description = "账号注册")
    public Map<String, Object> register(ClientParams clientParams) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 账号登录
     * <p>
     * 流程：验证账号 → 分配 Game/World 服务器 → 选择 Gate → 生成 Token → 返回连接信息
     * <p>
     * 返回: {code, token, uid, gateHost, gatePort, gameServerId}
     */
    @GmApi(path = "/login", method = HttpMethod.POST, description = "账号登录")
    public Map<String, Object> login(ClientParams clientParams) {
        Map<String, Object> result = new HashMap<>();

        String account = clientParams.getAccount();
        String password = clientParams.getPassword();

        if (account == null || account.isEmpty()) {
            result.put("code", 400);
            result.put("message", "account required");
            return result;
        }

        // TODO: 实际的账号密码验证逻辑（查库等）
        long uid = allocateUid(account);

        // 从 ZK 动态分配 Game 服务器
        ServiceInstance gameInstance = selectGameServer(uid);
        if (gameInstance == null) {
            result.put("code", 503);
            result.put("message", "no game server available");
            return result;
        }
        int gameServerId = parseServerId(gameInstance);

        // 从 ZK 选择负载最低的 Gate
        ServiceInstance gateInstance = selectGateServer(uid);
        if (gateInstance == null) {
            result.put("code", 503);
            result.put("message", "no gate server available");
            return result;
        }
        String gateHost = gateInstance.getMetadata(ServiceInstance.META_TCP_HOST);
        int gatePort = Integer.parseInt(
                gateInstance.getMetadata(ServiceInstance.META_TCP_PORT, "9001"));

        String token = tokenService.generateToken(uid, account, gameServerId);

        result.put("code", 0);
        result.put("token", token);
        result.put("uid", uid);
        result.put("gateHost", gateHost);
        result.put("gatePort", gatePort);
        result.put("gameServerId", gameServerId);

        log.info("Player login: account={}, uid={}, game={}, gate={}:{}",
                account, uid, gameServerId, gateHost, gatePort);
        return result;
    }

    /**
     * 获取服务器列表
     */
    @GmApi(path = "/server-list", description = "获取服务器列表")
    public Map<String, Object> serverList(@Param("token") String token) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 选择服务器
     */
    @GmApi(path = "/select-server", method = HttpMethod.POST, description = "选择服务器")
    public Map<String, Object> selectServer(
            @Param("token") String token,
            @Param("serverId") int serverId) {
        throw new UnsupportedOperationException("待实现");
    }

    /**
     * 验证 Token（HTTP 方式，供外部调用）
     */
    @GmApi(path = "/verify-token", description = "验证Token")
    public Map<String, Object> verifyToken(@Param("token") String token) {
        Map<String, Object> result = new HashMap<>();
        TokenInfo info = TokenStore.verify(globalRedis, token);
        if (info == null) {
            result.put("code", 401);
            result.put("message", "token invalid or expired");
        } else {
            result.put("code", 0);
            result.put("uid", info.getUid());
            result.put("gameServerId", info.getGameServerId());
        }
        return result;
    }

    // ==================== 服务发现（子类可重写定制策略） ====================

    /**
     * 选择 Game 服务器实例。默认按负载选择。
     */
    protected ServiceInstance selectGameServer(long uid) {
        if (rpcProxy == null) {
            return null;
        }
        List<ServiceInstance> instances = rpcProxy.getInstances(IGameService.class);
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        return selectByLoad(instances);
    }

    /**
     * 选择 Gate 服务器实例。默认按负载选择。
     */
    protected ServiceInstance selectGateServer(long uid) {
        if (rpcProxy == null) {
            return null;
        }
        List<ServiceInstance> instances = rpcProxy.getInstances(IGateService.class);
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        return selectByLoad(instances);
    }

    /**
     * 从 ServiceInstance 解析 serverId
     */
    protected int parseServerId(ServiceInstance instance) {
        String key = instance.getServiceKey();
        int idx = key.lastIndexOf('#');
        if (idx >= 0) {
            try {
                return Integer.parseInt(key.substring(idx + 1));
            } catch (NumberFormatException e) {
                // fall through
            }
        }
        return 1;
    }

    /**
     * 按负载选择（取 currentConnections 最小的实例）
     */
    private ServiceInstance selectByLoad(List<ServiceInstance> instances) {
        ServiceInstance best = null;
        int minLoad = Integer.MAX_VALUE;
        for (ServiceInstance inst : instances) {
            String loadStr = inst.getMetadata(ServiceInstance.META_CURRENT_CONNECTIONS, "0");
            int load = Integer.parseInt(loadStr);
            if (load < minLoad) {
                minLoad = load;
                best = inst;
            }
        }
        return best != null ? best : instances.getFirst();
    }

    // ==================== 子类重写以实现具体分配逻辑 ====================

    protected long allocateUid(String account) {
        return account.hashCode() & 0x7FFFFFFFL;
    }
}
