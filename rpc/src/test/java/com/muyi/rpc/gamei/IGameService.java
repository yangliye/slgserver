package com.muyi.rpc.gamei;

import com.muyi.rpc.annotation.RpcTimeout;
import com.muyi.rpc.core.RpcFuture;

/**
 * Game 服务接口
 * 用于 Game -> Game 的跨服 RPC 调用
 *
 * @author muyi
 */
public interface IGameService {
    
    /**
     * 获取玩家信息
     */
    @RpcTimeout(2000)
    PlayerInfo getPlayerInfo(long playerId);
    
    /**
     * 跨服发送消息给玩家
     */
    void sendMessageToPlayer(long playerId, String message);
    
    /**
     * 跨服转账（玩家间转移金币）- 同步
     */
    @RpcTimeout(value = 10000, retries = 0)
    TransferResult transferGold(long fromPlayerId, long toPlayerId, long amount);
    
    /**
     * 跨服转账（玩家间转移金币）- 异步
     * 返回 RpcFuture，调用方可以异步等待结果或注册回调
     */
    @RpcTimeout(value = 10000, retries = 0)
    RpcFuture transferGoldAsync(long fromPlayerId, long toPlayerId, long amount);
    
    /**
     * 扣除玩家金币（用于跨服转账场景）
     * 
     * @param playerId 玩家ID
     * @param amount   扣除金额
     * @return 扣除结果，包含实际扣除金额和玩家剩余金额
     */
    @RpcTimeout(5000)
    DeductResult deductGold(long playerId, long amount);
    
    /**
     * 扣除玩家金币 - 异步版本
     */
    @RpcTimeout(5000)
    RpcFuture deductGoldAsync(long playerId, long amount);
    
    /**
     * 跨服查询玩家是否在线
     */
    @RpcTimeout(1000)
    boolean isPlayerOnline(long playerId);
}
