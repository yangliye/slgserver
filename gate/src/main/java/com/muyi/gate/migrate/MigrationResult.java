package com.muyi.gate.migrate;

import java.io.Serializable;

/**
 * 迁服结果
 *
 * @author muyi
 */
public class MigrationResult implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 是否成功 */
    private boolean success;
    
    /** 玩家ID */
    private long playerId;
    
    /** 目标服务器ID */
    private int targetServerId;
    
    /** 错误码 */
    private int errorCode;
    
    /** 错误信息 */
    private String errorMessage;
    
    public MigrationResult() {
    }
    
    public static MigrationResult success(long playerId, int targetServerId) {
        MigrationResult result = new MigrationResult();
        result.success = true;
        result.playerId = playerId;
        result.targetServerId = targetServerId;
        return result;
    }
    
    public static MigrationResult fail(long playerId, int errorCode, String errorMessage) {
        MigrationResult result = new MigrationResult();
        result.success = false;
        result.playerId = playerId;
        result.errorCode = errorCode;
        result.errorMessage = errorMessage;
        return result;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public long getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }
    
    public int getTargetServerId() {
        return targetServerId;
    }
    
    public void setTargetServerId(int targetServerId) {
        this.targetServerId = targetServerId;
    }
    
    public int getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    @Override
    public String toString() {
        if (success) {
            return "MigrationResult{success, playerId=" + playerId + ", targetServerId=" + targetServerId + '}';
        }
        return "MigrationResult{failed, playerId=" + playerId + ", error=" + errorCode + ", msg=" + errorMessage + '}';
    }
}
