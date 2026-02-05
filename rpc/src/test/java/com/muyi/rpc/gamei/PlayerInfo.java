package com.muyi.rpc.gamei;

import java.io.Serializable;

/**
 * 玩家信息
 *
 * @author muyi
 */
public class PlayerInfo implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private long playerId;
    private String playerName;
    private int level;
    private long gold;
    private int diamond;
    private int vipLevel;
    private String serverName;
    private long lastLoginTime;
    private boolean online;
    
    public PlayerInfo() {
    }
    
    public PlayerInfo(long playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
    }
    
    // Getters and Setters
    
    public long getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = level;
    }
    
    public long getGold() {
        return gold;
    }
    
    public void setGold(long gold) {
        this.gold = gold;
    }
    
    public int getDiamond() {
        return diamond;
    }
    
    public void setDiamond(int diamond) {
        this.diamond = diamond;
    }

    public int getVipLevel() {
        return vipLevel;
    }
    
    public void setVipLevel(int vipLevel) {
        this.vipLevel = vipLevel;
    }
    
    public String getServerName() {
        return serverName;
    }
    
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }
    
    public long getLastLoginTime() {
        return lastLoginTime;
    }
    
    public void setLastLoginTime(long lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }
    
    public boolean isOnline() {
        return online;
    }
    
    public void setOnline(boolean online) {
        this.online = online;
    }
    
    @Override
    public String toString() {
        return "PlayerInfo{" +
                "playerId=" + playerId +
                ", playerName='" + playerName + '\'' +
                ", level=" + level +
                ", gold=" + gold +
                ", vipLevel=" + vipLevel +
                ", online=" + online +
                '}';
    }
}

