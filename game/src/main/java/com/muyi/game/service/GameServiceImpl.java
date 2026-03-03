package com.muyi.game.service;

import com.muyi.game.handler.GameMessageDispatcher;
import com.muyi.game.player.PlayerExecutor;
import com.muyi.game.player.PlayerExecutorManager;
import com.muyi.rpc.annotation.RpcService;
import com.muyi.shared.api.IGameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Game RPC 服务实现
 * <p>
 * 处理 gate 发来的玩家生命周期事件和消息转发。
 * {@link #forwardMessage} 支持 PlayerExecutor 懒创建：
 * 第一条业务消息到达时自动创建 PlayerExecutor。
 *
 * @author muyi
 */
@RpcService(IGameService.class)
public class GameServiceImpl implements IGameService {

    private static final Logger log = LoggerFactory.getLogger(GameServiceImpl.class);

    private final PlayerExecutorManager playerExecutorManager;
    private GameMessageDispatcher messageDispatcher;

    public GameServiceImpl(PlayerExecutorManager playerExecutorManager) {
        this.playerExecutorManager = playerExecutorManager;
    }

    public void setMessageDispatcher(GameMessageDispatcher messageDispatcher) {
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public boolean playerLogin(long uid, String authToken, int gateServerId) {
        try {
            playerExecutorManager.bind(uid, authToken, gateServerId);
            log.info("Player[{}] login from gate-{}", uid, gateServerId);
            return true;
        } catch (Exception e) {
            log.error("Player[{}] login failed", uid, e);
            return false;
        }
    }

    @Override
    public boolean playerLogout(long uid) {
        boolean result = playerExecutorManager.unbind(uid);
        if (result) {
            log.info("Player[{}] logout", uid);
        } else {
            log.warn("Player[{}] logout but not found", uid);
        }
        return result;
    }

    @Override
    public boolean playerReconnect(long uid, String newAuthToken, int newGateServerId) {
        PlayerExecutor pe = playerExecutorManager.get(uid);
        if (pe == null) {
            log.info("Player[{}] reconnect but not found, treating as new login", uid);
            return playerLogin(uid, newAuthToken, newGateServerId);
        }

        pe.getToken().refreshToken(newAuthToken, newGateServerId);
        log.info("Player[{}] reconnected to gate-{}", uid, newGateServerId);
        return true;
    }

    @Override
    public boolean isOnline(long uid) {
        return playerExecutorManager.isOnline(uid);
    }

    @Override
    public int getOnlineCount() {
        return playerExecutorManager.getOnlineCount();
    }

    @Override
    public void forwardMessage(long uid, String authToken, int gateServerId,
                               int msgId, int msgSeq, byte[] payload) {
        if (!playerExecutorManager.isOnline(uid)) {
            playerExecutorManager.bind(uid, authToken, gateServerId);
            log.info("Player[{}] lazily created via forwardMessage, gate={}", uid, gateServerId);
        }

        if (messageDispatcher != null) {
            messageDispatcher.dispatch(uid, msgId, msgSeq, payload);
        } else {
            log.warn("Player[{}] messageDispatcher not set, msgId={} dropped", uid, msgId);
        }
    }
}
