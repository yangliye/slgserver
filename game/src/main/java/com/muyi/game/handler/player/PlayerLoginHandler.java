package com.muyi.game.handler.player;

import com.muyi.common.util.time.TimeUtils;
import com.muyi.core.module.ModuleContext;
import com.muyi.game.data.PlayerDataContext;
import com.muyi.game.data.PlayerDataRegistry;
import com.muyi.game.handler.MessageHandler;
import com.muyi.game.manager.HeroManager;
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
 * 负责创建 {@link PlayerDataContext}、加载所有玩家数据并下发给客户端。
 * <p>
 * 完整链路：
 * <pre>
 * Client → Gate(TCP) → GameServiceImpl.forwardMessage(RPC)
 *     → PlayerExecutor → GameMessageDispatcher
 *     → PlayerLoginHandler.handle()
 *     → 创建 PlayerDataContext → loadAll → onLogin → pushToGate 响应
 * </pre>
 */
public class PlayerLoginHandler implements MessageHandler<PlayerLoginReq> {

    private static final Logger log = LoggerFactory.getLogger(PlayerLoginHandler.class);

    private static final int RESP_MSG_ID = MsgId.PLAYER_LOGIN_RESP_VALUE;

    private final PlayerExecutorManager playerManager;
    private final PlayerDataRegistry dataRegistry;

    public PlayerLoginHandler(PlayerExecutorManager playerManager, PlayerDataRegistry dataRegistry) {
        this.playerManager = playerManager;
        this.dataRegistry = dataRegistry;
    }

    @Override
    public void handle(long uid, int msgSeq, PlayerLoginReq msg) {
        PlayerExecutor executor = playerManager.get(uid);
        if (executor == null) {
            log.error("Player[{}] login handler invoked but executor not found", uid);
            return;
        }

        long startTime = TimeUtils.currentTimeMillis();

        // 1. 创建数据上下文并加载所有玩家数据
        PlayerDataContext dataContext = dataRegistry.createContext(uid);
        dataContext.loadAll();
        dataContext.onLogin();
        executor.setDataContext(dataContext);

        long loadCost = TimeUtils.currentTimeMillis() - startTime;

        // 2. 构建登录响应（从已加载的 Manager 中读取数据）
        PlayerData playerData = buildPlayerData(uid, dataContext);

        PlayerLoginResp resp = PlayerLoginResp.newBuilder()
                .setCode(0)
                .setMessage("ok")
                .setPlayerData(playerData)
                .build();

        executor.pushToGate(RESP_MSG_ID, resp.toByteArray());

        HeroManager heroMgr = dataContext.getManager(HeroManager.class);
        log.info("Player[{}] entered game, heroes={}, loadCost={}ms, module={}-{}",
                uid, heroMgr != null ? heroMgr.getHeroCount() : 0, loadCost,
                ModuleContext.current().name(), ModuleContext.current().getServerId());
    }

    /**
     * 从玩家数据上下文构建 Protobuf 响应
     */
    private PlayerData buildPlayerData(long uid, PlayerDataContext dataContext) {
        // TODO: 从各 Manager 收集数据构建完整的 PlayerData proto
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
