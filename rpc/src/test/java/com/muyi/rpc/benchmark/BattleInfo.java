package com.muyi.rpc.benchmark;

import java.io.Serializable;

/**
 * 战况信息
 * 
 * @author muyi
 */
public class BattleInfo implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private int totalPlayers;
    private int alliance1Score;
    private int alliance2Score;
    private long battleStartTime;
    private int phase;  // 战斗阶段
    
    public BattleInfo() {
    }
    
    public BattleInfo(int totalPlayers, int alliance1Score, int alliance2Score, 
                      long battleStartTime, int phase) {
        this.totalPlayers = totalPlayers;
        this.alliance1Score = alliance1Score;
        this.alliance2Score = alliance2Score;
        this.battleStartTime = battleStartTime;
        this.phase = phase;
    }
    
    // Getters and Setters
    
    public int getTotalPlayers() {
        return totalPlayers;
    }
    
    public void setTotalPlayers(int totalPlayers) {
        this.totalPlayers = totalPlayers;
    }
    
    public int getAlliance1Score() {
        return alliance1Score;
    }
    
    public void setAlliance1Score(int alliance1Score) {
        this.alliance1Score = alliance1Score;
    }
    
    public int getAlliance2Score() {
        return alliance2Score;
    }
    
    public void setAlliance2Score(int alliance2Score) {
        this.alliance2Score = alliance2Score;
    }
    
    public long getBattleStartTime() {
        return battleStartTime;
    }
    
    public void setBattleStartTime(long battleStartTime) {
        this.battleStartTime = battleStartTime;
    }
    
    public int getPhase() {
        return phase;
    }
    
    public void setPhase(int phase) {
        this.phase = phase;
    }
}
