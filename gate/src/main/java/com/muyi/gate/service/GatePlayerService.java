package com.muyi.gate.service;

import com.muyi.gate.session.Session;
import com.muyi.gate.session.SessionManager;
import com.muyi.rpc.client.RpcProxyManager;
import com.muyi.shared.api.IGameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gate 玩家生命周期服务
 * <p>
 * 封装玩家在 gate 上的完整登录/登出/重连流程，
 * 负责协调 Session 管理和通知 game 模块。
 * <p>
 * 典型调用流程：
 * <pre>{@code
 * // 客户端发来登录请求，gate handler 中调用
 * gatePlayerService.onPlayerLogin(session, playerId, account, gameServerId, authToken);
 *
 * // 客户端断线，gate handler 中调用
 * gatePlayerService.onPlayerDisconnect(session);
 * }</pre>
 */
public class GatePlayerService {

    private static final Logger log = LoggerFactory.getLogger(GatePlayerService.class);

    private final SessionManager sessionManager;
    private final RpcProxyManager rpcProxy;

    public GatePlayerService(SessionManager sessionManager, RpcProxyManager rpcProxy) {
        this.sessionManager = sessionManager;
        this.rpcProxy = rpcProxy;
    }

    /**
     * 玩家登录（认证成功后由 gate handler 调用）
     *
     * @param session      玩家 session
     * @param playerId     玩家 ID
     * @param account      账号
     * @param gameServerId 分配的 game 服务器 ID
     * @param worldServerId 分配的 world 服务器 ID
     * @param authToken    认证凭据（传给 game 用于后续校验）
     * @return 是否成功
     */
    public boolean onPlayerLogin(Session session, long playerId, String account,
                                 int gameServerId, String authToken) {
        if (!session.authenticate(playerId, account)) {
            log.warn("Player[{}] authenticate failed, state={}", playerId, session.getState());
            return false;
        }
        sessionManager.bindPlayerId(session, playerId);

        if (!session.enterGame(gameServerId)) {
            log.warn("Player[{}] enterGame failed, state={}", playerId, session.getState());
            return false;
        }

        try {
            int gateServerId = getGateServerId();
            IGameService gameService = rpcProxy.get(IGameService.class, gameServerId);
            boolean result = gameService.playerLogin(playerId, authToken, gateServerId);
            if (!result) {
                log.warn("Player[{}] game.playerLogin returned false", playerId);
            }
        } catch (Exception e) {
            log.error("Player[{}] notify game login failed", playerId, e);
        }

        log.info("Player[{}] login success, game={}", playerId, gameServerId);
        return true;
    }

    /**
     * 玩家断线（连接关闭时由 gate handler 调用）
     */
    public void onPlayerDisconnect(Session session) {
        long playerId = session.getPlayerId();
        if (playerId <= 0) {
            sessionManager.removeSession(session);
            return;
        }

        // 1. 通知 game 模块清理 PlayerExecutor
        int gameServerId = session.getGameServerId();
        if (gameServerId > 0) {
            try {
                IGameService gameService = rpcProxy.get(IGameService.class, gameServerId);
                gameService.playerLogout(playerId);
            } catch (Exception e) {
                log.error("Player[{}] notify game logout failed", playerId, e);
            }
        }

        // 2. 清理 session
        sessionManager.removeSession(session);
        log.info("Player[{}] disconnected", playerId);
    }

    /**
     * 玩家重连（换了 gate 或 session 后由 handler 调用）
     */
    public boolean onPlayerReconnect(Session session, long playerId, String account,
                                     int gameServerId, String newAuthToken) {
        if (!session.authenticate(playerId, account)) {
            log.warn("Player[{}] reconnect authenticate failed", playerId);
            return false;
        }
        sessionManager.bindPlayerId(session, playerId);

        if (!session.enterGame(gameServerId)) {
            log.warn("Player[{}] reconnect enterGame failed", playerId);
            return false;
        }

        try {
            int gateServerId = getGateServerId();
            IGameService gameService = rpcProxy.get(IGameService.class, gameServerId);
            gameService.playerReconnect(playerId, newAuthToken, gateServerId);
        } catch (Exception e) {
            log.error("Player[{}] notify game reconnect failed", playerId, e);
        }

        log.info("Player[{}] reconnected, game={}", playerId, gameServerId);
        return true;
    }

    private int getGateServerId() {
        // TODO: 从 gate 模块配置获取本 gate 的 serverId
        return 0;
    }
}
