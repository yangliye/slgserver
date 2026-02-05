package com.muyi.rpc.benchmark;

import com.muyi.rpc.annotation.RpcTimeout;

/**
 * 战场服务接口
 * 
 * @author muyi
 */
public interface IBattleService {
    
    /**
     * 心跳检测
     */
    @RpcTimeout(1000)
    String ping();
    
    /**
     * 玩家进入战场
     */
    @RpcTimeout(3000)
    boolean enterBattle(long playerId, int allianceId);
    
    /**
     * 同步玩家状态
     */
    @RpcTimeout(1000)
    void syncState(long playerId, int x, int y, int hp);
    
    /**
     * 使用技能
     */
    @RpcTimeout(2000)
    int useSkill(long attackerId, long targetId, int skillId);
    
    /**
     * 获取战况信息
     */
    @RpcTimeout(1000)
    BattleInfo getBattleInfo();
    
    /**
     * 玩家离开战场
     */
    @RpcTimeout(2000)
    void leaveBattle(long playerId);
}
