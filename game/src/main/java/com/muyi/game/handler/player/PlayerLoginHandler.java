package com.muyi.game.handler.player;

import com.muyi.common.util.time.TimeUtils;
import com.muyi.core.module.ModuleContext;
import com.muyi.game.handler.MessageHandler;
import com.muyi.game.player.PlayerExecutor;
import com.muyi.game.player.PlayerExecutorManager;
import com.muyi.proto.MsgId;
import com.muyi.proto.game.PlayerData;
import com.muyi.proto.game.PlayerLoginReq;
import com.muyi.proto.game.PlayerLoginResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 玩家登录（进入游戏）Handler
 * <p>
 * 客户端通过 Gate 认证后，发送的第一条业务消息。
 * 负责加载玩家数据并下发给客户端。
 * <p>
 * 完整链路：
 * <pre>
 * Client → Gate(TCP) → GameServiceImpl.forwardMessage(RPC)
 *     → PlayerExecutor 懒创建 → GameMessageDispatcher
 *     → PlayerLoginHandler.handle()
 *     → 加载数据 → pushToGate 响应
 * </pre>
 */
public class PlayerLoginHandler implements MessageHandler<PlayerLoginReq> {

    private static final Logger log = LoggerFactory.getLogger(PlayerLoginHandler.class);

    private static final int RESP_MSG_ID = MsgId.PLAYER_LOGIN_RESP_VALUE;

    private final PlayerExecutorManager playerManager;

    public PlayerLoginHandler(PlayerExecutorManager playerManager) {
        this.playerManager = playerManager;
    }

    @Override
    public void handle(long uid, int msgSeq, PlayerLoginReq msg) {
        PlayerExecutor executor = playerManager.get(uid);
        if (executor == null) {
            log.error("Player[{}] login handler invoked but executor not found", uid);
            return;
        }

        // TODO: 从 DB 加载玩家数据，此处用模拟数据演示
        PlayerData playerData = loadPlayerData(uid);

        PlayerLoginResp resp = PlayerLoginResp.newBuilder()
                .setCode(0)
                .setMessage("ok")
                .setPlayerData(playerData)
                .build();

        executor.pushToGate(RESP_MSG_ID, resp.toByteArray());
        log.info("Player[{}] entered game, level={}, module={}-{}",
                uid, playerData.getLevel(),
                ModuleContext.current().name(), ModuleContext.current().getServerId());
    }

    /**
     * 加载玩家数据
     * <p>
     * TODO: 替换为真实 DB 查询。新玩家走创建流程。
     */
    private PlayerData loadPlayerData(long uid) {
        long now = TimeUtils.currentTimeMillis();
        return PlayerData.newBuilder()
                .setUid(uid)
                .setName("Player_" + uid)
                .setLevel(1)
                .setExp(0)
                .setVipLevel(0)
                .setGold(10000)
                .setDiamond(100)
                .setLastLoginTime(now)
                .setCreateTime(now)
                .build();
    }
}
