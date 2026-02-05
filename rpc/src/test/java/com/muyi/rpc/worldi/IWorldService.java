package com.muyi.rpc.worldi;

import com.muyi.rpc.annotation.RpcTimeout;

/**
 * 世界服务接口
 * 用于 Game -> World 的 RPC 调用
 *
 * @author muyi
 */
public interface IWorldService {
    
    /**
     * 玩家进入世界（使用默认超时 5000ms）
     */
    WorldEnterResult enterWorld(long playerId, int worldId);
    
    /**
     * 获取世界信息（快速查询，较短超时）
     */
    @RpcTimeout(2000)
    WorldInfo getWorldInfo(int worldId);
    
    /**
     * 玩家离开世界（单向调用，无超时）
     */
    void leaveWorld(long playerId);
    
    /**
     * 广播消息到世界（单向调用，无超时）
     */
    void broadcastToWorld(int worldId, String message);
}
