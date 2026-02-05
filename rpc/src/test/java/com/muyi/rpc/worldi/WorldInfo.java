package com.muyi.rpc.worldi;

import java.io.Serializable;

/**
 * 世界信息
 *
 * @author muyi
 */
public class WorldInfo implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private int worldId;
    private String worldName;
    private int onlineCount;
    private int maxPlayers;
    private String serverAddress;
    
    public WorldInfo() {
    }
    
    public WorldInfo(int worldId, String worldName) {
        this.worldId = worldId;
        this.worldName = worldName;
    }
    
    // Getters and Setters
    
    public int getWorldId() {
        return worldId;
    }
    
    public void setWorldId(int worldId) {
        this.worldId = worldId;
    }
    
    public String getWorldName() {
        return worldName;
    }
    
    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }
    
    public int getOnlineCount() {
        return onlineCount;
    }
    
    public void setOnlineCount(int onlineCount) {
        this.onlineCount = onlineCount;
    }
    
    public int getMaxPlayers() {
        return maxPlayers;
    }
    
    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }
    
    public String getServerAddress() {
        return serverAddress;
    }
    
    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }
    
    @Override
    public String toString() {
        return "WorldInfo{" +
                "worldId=" + worldId +
                ", worldName='" + worldName + '\'' +
                ", onlineCount=" + onlineCount +
                ", maxPlayers=" + maxPlayers +
                '}';
    }
}

