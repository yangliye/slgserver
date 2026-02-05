package com.muyi.rpc.benchmark;

import com.muyi.rpc.annotation.RpcService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 战场服务实现
 * 
 * @author muyi
 */
@RpcService(IBattleService.class)
public class BattleServiceImpl implements IBattleService {
    
    // 玩家数据（简化模拟）
    private final ConcurrentHashMap<Long, PlayerState> players = new ConcurrentHashMap<>();
    private final AtomicInteger totalPlayers = new AtomicInteger(0);
    private final AtomicInteger alliance1Score = new AtomicInteger(0);
    private final AtomicInteger alliance2Score = new AtomicInteger(0);
    private final long battleStartTime = System.currentTimeMillis();
    
    @Override
    public String ping() {
        return "pong";
    }
    
    @Override
    public boolean enterBattle(long playerId, int allianceId) {
        PlayerState state = new PlayerState(playerId, allianceId);
        players.put(playerId, state);
        totalPlayers.incrementAndGet();
        return true;
    }
    
    @Override
    public void syncState(long playerId, int x, int y, int hp) {
        PlayerState state = players.get(playerId);
        if (state != null) {
            state.x = x;
            state.y = y;
            state.hp = hp;
        }
    }
    
    @Override
    public int useSkill(long attackerId, long targetId, int skillId) {
        // 模拟技能伤害计算
        int damage = ThreadLocalRandom.current().nextInt(100, 500);
        
        PlayerState attacker = players.get(attackerId);
        PlayerState target = players.get(targetId);
        
        if (attacker != null && target != null) {
            target.hp = Math.max(0, target.hp - damage);
            
            // 更新联盟积分
            if (target.hp == 0) {
                if (attacker.allianceId % 2 == 0) {
                    alliance1Score.incrementAndGet();
                } else {
                    alliance2Score.incrementAndGet();
                }
            }
        }
        
        return damage;
    }
    
    @Override
    public BattleInfo getBattleInfo() {
        return new BattleInfo(
                totalPlayers.get(),
                alliance1Score.get(),
                alliance2Score.get(),
                battleStartTime,
                1
        );
    }
    
    @Override
    public void leaveBattle(long playerId) {
        if (players.remove(playerId) != null) {
            totalPlayers.decrementAndGet();
        }
    }
    
    /**
     * 玩家状态
     */
    private static class PlayerState {
        final long playerId;
        final int allianceId;
        volatile int x;
        volatile int y;
        volatile int hp = 10000;
        
        PlayerState(long playerId, int allianceId) {
            this.playerId = playerId;
            this.allianceId = allianceId;
        }
    }
}
