package com.muyi.rpc.benchmark;

import com.muyi.rpc.Rpc;
import com.muyi.rpc.annotation.RpcService;

/**
 * 游戏服战斗服务实现
 * 
 * @author muyi
 */
@RpcService(IGameBattleService.class)
public class GameBattleServiceImpl implements IGameBattleService {
    
    private final int serverId;
    
    public GameBattleServiceImpl(int serverId) {
        this.serverId = serverId;
    }
    
    @Override
    public boolean supportBattle(long playerId, int battleServerId) {
        // 模拟跨服支援逻辑
        // 1. 检查玩家是否可以支援
        // 2. 调用战场服务器的 enterBattle
        try {
            IBattleService battleService = Rpc.getClient().get(IBattleService.class, battleServerId);
            if (battleService != null) {
                int allianceId = (int) (playerId % 10);
                return battleService.enterBattle(playerId, allianceId);
            }
        } catch (Exception e) {
            // 跨服调用失败
        }
        return false;
    }
    
    @Override
    public long[] getAvailableSupporters(int allianceId) {
        // 模拟返回可支援的玩家列表
        long[] supporters = new long[100];
        for (int i = 0; i < 100; i++) {
            supporters[i] = serverId * 100000L + allianceId * 1000L + i;
        }
        return supporters;
    }
}
