package com.muyi.game.player;

/**
 * Gate 消息推送接口
 * <p>
 * 将推送能力与 RPC 实现解耦，由 game 模块初始化时注入具体实现。
 * <p>
 * 典型实现：
 * <pre>{@code
 * GatePusher pusher = (gateServerId, playerId, protoId, message) -> {
 *     IGateService gate = rpcProxy.get(IGateService.class, gateServerId);
 *     gate.pushMessage(playerId, protoId, message);
 * };
 * }</pre>
 */
@FunctionalInterface
public interface GatePusher {

    /**
     * 向指定 gate 推送消息给玩家
     *
     * @param gateServerId 目标 gate 服务器 ID
     * @param playerId     玩家 ID
     * @param protoId      协议 ID
     * @param message      消息体
     */
    void push(int gateServerId, long playerId, int protoId, Object message);
}
