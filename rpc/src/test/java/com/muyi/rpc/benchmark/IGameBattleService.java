package com.muyi.rpc.benchmark;

import com.muyi.rpc.annotation.RpcTimeout;

/**
 * 游戏服战斗相关接口
 * 用于跨服支援等场景
 * 
 * @author muyi
 */
public interface IGameBattleService {
    
    /**
     * 支援战场
     * 
     * @param playerId 玩家ID
     * @param battleServerId 战场服务器ID
     * @return 是否成功
     */
    @RpcTimeout(5000)
    boolean supportBattle(long playerId, int battleServerId);
    
    /**
     * 获取可支援玩家列表
     */
    @RpcTimeout(2000)
    long[] getAvailableSupporters(int allianceId);
}
