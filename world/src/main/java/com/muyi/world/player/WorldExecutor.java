package com.muyi.world.player;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 玩家在 World 中的执行器
 * <p>
 * 比 Game 侧的 PlayerExecutor 更轻量，仅维护路由信息和推送能力，
 * 不持有玩家数据上下文（World 的数据是全局共享的，不按玩家分库）。
 */
public class WorldExecutor {

    private static final Logger log = LoggerFactory.getLogger(WorldExecutor.class);

    private final long uid;
    private final int gameServerId;
    private final int gateServerId;
    private final WorldGatePusher gatePusher;

    public WorldExecutor(long uid, int gameServerId, int gateServerId, WorldGatePusher gatePusher) {
        this.uid = uid;
        this.gameServerId = gameServerId;
        this.gateServerId = gateServerId;
        this.gatePusher = gatePusher;
    }

    /**
     * 向玩家推送消息（通过 gate）
     */
    public void pushToGate(int protoId, Object message) {
        if (gateServerId <= 0) {
            log.warn("Player[{}] has no gate connection in world, push dropped", uid);
            return;
        }
        try {
            gatePusher.push(gateServerId, uid, protoId, message);
        } catch (Exception e) {
            log.error("Player[{}] push to gate-{} failed in world, protoId={}",
                    uid, gateServerId, protoId, e);
        }
    }

    public long getUid() {
        return uid;
    }

    public int getGameServerId() {
        return gameServerId;
    }

    public int getGateServerId() {
        return gateServerId;
    }
}
