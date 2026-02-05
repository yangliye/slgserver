package com.muyi.rpc.worldi;

import java.io.Serializable;

/**
 * 进入世界结果
 *
 * @author muyi
 */
public class WorldEnterResult implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private boolean success;
    private String message;
    private WorldInfo worldInfo;
    private long enterTime;
    
    public WorldEnterResult() {
    }
    
    public static WorldEnterResult success(WorldInfo worldInfo) {
        WorldEnterResult result = new WorldEnterResult();
        result.success = true;
        result.message = "Enter world success";
        result.worldInfo = worldInfo;
        result.enterTime = System.currentTimeMillis();
        return result;
    }
    
    public static WorldEnterResult fail(String message) {
        WorldEnterResult result = new WorldEnterResult();
        result.success = false;
        result.message = message;
        return result;
    }
    
    // Getters and Setters
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public WorldInfo getWorldInfo() {
        return worldInfo;
    }
    
    public void setWorldInfo(WorldInfo worldInfo) {
        this.worldInfo = worldInfo;
    }
    
    public long getEnterTime() {
        return enterTime;
    }
    
    public void setEnterTime(long enterTime) {
        this.enterTime = enterTime;
    }
    
    @Override
    public String toString() {
        return "WorldEnterResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", worldInfo=" + worldInfo +
                '}';
    }
}

