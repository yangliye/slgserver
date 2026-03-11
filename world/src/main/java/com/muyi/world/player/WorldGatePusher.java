package com.muyi.world.player;

/**
 * World 侧 Gate 推送接口
 */
@FunctionalInterface
public interface WorldGatePusher {

    void push(int gateServerId, long playerId, int protoId, Object message);
}
