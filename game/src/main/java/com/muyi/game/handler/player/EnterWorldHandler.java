package com.muyi.game.handler.player;

import com.muyi.core.handler.MessageHandler;
import com.muyi.core.handler.MsgHandler;
import com.muyi.core.module.AbstractGameModule;
import com.muyi.core.module.ModuleContext;
import com.muyi.game.GameModule;
import com.muyi.game.player.PlayerExecutor;
import com.muyi.game.player.PlayerExecutorManager;
import com.muyi.proto.MsgId;
import com.muyi.proto.game.EnterWorldReq;
import com.muyi.proto.game.EnterWorldResp;
import com.muyi.shared.api.gate.IGateService;
import com.muyi.shared.api.world.IWorldService;
import com.muyi.rpc.client.RpcProxyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 进入大世界 Handler
 * <p>
 * 玩家登录完成后请求进入大世界，Game 负责分配 world 并通知 Gate 绑定路由。
 * <p>
 * 当前策略：worldServerId = 玩家所在 gameServerId（后续可扩展为动态分配）
 */
@MsgHandler(MsgId.ENTER_WORLD_REQ_VALUE)
public class EnterWorldHandler implements MessageHandler<EnterWorldReq> {

    private static final Logger log = LoggerFactory.getLogger(EnterWorldHandler.class);

    private PlayerExecutorManager playerManager;
    private RpcProxyManager rpcProxy;

    @Override
    public void init(AbstractGameModule module) {
        GameModule game = (GameModule) module;
        this.playerManager = game.getPlayerExecutorManager();
        this.rpcProxy = module.getRpcProxy();
    }

    @Override
    public void handle(long uid, int msgSeq, EnterWorldReq msg) {
        PlayerExecutor executor = playerManager.get(uid);
        if (executor == null) {
            log.error("Player[{}] enter world but executor not found", uid);
            return;
        }

        int gameServerId = ModuleContext.current().getServerId();
        int worldServerId = gameServerId;

        // 1. 通知 World 初始化玩家数据
        if (rpcProxy != null) {
            try {
                IWorldService worldService = rpcProxy.get(IWorldService.class, worldServerId);
                if (!worldService.enterWorld(uid, gameServerId)) {
                    log.error("Player[{}] enter world-{} rejected", uid, worldServerId);
                    executor.pushToGate(MsgId.ENTER_WORLD_RESP_VALUE, EnterWorldResp.newBuilder()
                            .setCode(2).setMessage("enter world rejected").build().toByteArray());
                    return;
                }
            } catch (Exception e) {
                log.error("Player[{}] enter world-{} rpc failed", uid, worldServerId, e);
                executor.pushToGate(MsgId.ENTER_WORLD_RESP_VALUE, EnterWorldResp.newBuilder()
                        .setCode(3).setMessage("enter world failed").build().toByteArray());
                return;
            }
        }

        // 2. 通知 Gate 绑定 worldServerId（World 准备就绪后再绑定路由）
        int gateServerId = executor.getGateServerId();
        if (gateServerId > 0 && rpcProxy != null) {
            try {
                IGateService gateService = rpcProxy.get(IGateService.class, gateServerId);
                gateService.bindWorldServerId(uid, worldServerId);
            } catch (Exception e) {
                log.error("Player[{}] bind world to gate-{} failed", uid, gateServerId, e);
                executor.pushToGate(MsgId.ENTER_WORLD_RESP_VALUE, EnterWorldResp.newBuilder()
                        .setCode(1).setMessage("bind world failed").build().toByteArray());
                return;
            }
        }

        executor.getToken().setWorldServerId(worldServerId);

        executor.pushToGate(MsgId.ENTER_WORLD_RESP_VALUE, EnterWorldResp.newBuilder()
                .setCode(0).setMessage("ok").setWorldServerId(worldServerId)
                .build().toByteArray());

        log.info("Player[{}] entered world-{}", uid, worldServerId);
    }
}
