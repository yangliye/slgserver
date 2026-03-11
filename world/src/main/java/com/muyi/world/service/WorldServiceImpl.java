package com.muyi.world.service;

import com.muyi.rpc.annotation.RpcService;
import com.muyi.shared.api.world.IWorldService;
import com.muyi.world.handler.WorldMessageDispatcher;
import com.muyi.world.player.WorldExecutorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * World RPC 服务实现
 * <p>
 * 处理 Game/Gate 发来的 RPC 请求和消息转发。
 */
@RpcService(IWorldService.class)
public class WorldServiceImpl implements IWorldService {

    private static final Logger log = LoggerFactory.getLogger(WorldServiceImpl.class);

    private final WorldExecutorManager executorManager;
    private WorldMessageDispatcher messageDispatcher;

    public WorldServiceImpl(WorldExecutorManager executorManager) {
        this.executorManager = executorManager;
    }

    public void setMessageDispatcher(WorldMessageDispatcher messageDispatcher) {
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public boolean enterWorld(long uid, int gameServerId) {
        // TODO: 加载/初始化玩家在大世界的数据（坐标、城池等）
        log.info("Player[{}] entering world, game={}", uid, gameServerId);
        return true;
    }

    @Override
    public boolean leaveWorld(long uid) {
        boolean result = executorManager.unbind(uid);
        if (result) {
            log.info("Player[{}] left world", uid);
        }
        return result;
    }

    @Override
    public int[] getPlayerPosition(long uid) {
        // TODO: 从世界数据中查询
        return null;
    }

    @Override
    public boolean relocate(long uid, int x, int y) {
        // TODO: 迁城逻辑
        return false;
    }

    @Override
    public long[] getPlayersInArea(int x, int y, int radius) {
        // TODO: 区域查询
        return new long[0];
    }

    @Override
    public int getOnlineCount() {
        return executorManager.getOnlineCount();
    }

    @Override
    public void forwardMessage(long uid, int gameServerId, int gateServerId,
                               int msgId, int msgSeq, byte[] payload) {
        if (!executorManager.isOnline(uid)) {
            executorManager.bind(uid, gameServerId, gateServerId);
            log.info("Player[{}] lazily bound in world via forwardMessage, game={}, gate={}",
                    uid, gameServerId, gateServerId);
        }

        if (messageDispatcher != null) {
            messageDispatcher.dispatch(uid, msgId, msgSeq, payload);
        } else {
            log.warn("Player[{}] world messageDispatcher not set, msgId={} dropped", uid, msgId);
        }
    }
}
