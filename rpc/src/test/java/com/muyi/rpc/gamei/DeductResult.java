package com.muyi.rpc.gamei;

import java.io.Serializable;

/**
 * 扣除金币结果
 *
 * @author muyi
 */
public class DeductResult implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private boolean success;
    private String message;
    private long playerId;
    private long deductedAmount;    // 实际扣除金额
    private long remainingBalance;  // 剩余金额
    
    public static DeductResult success(long playerId, long deductedAmount, long remainingBalance) {
        DeductResult result = new DeductResult();
        result.success = true;
        result.message = "Deduct successful";
        result.playerId = playerId;
        result.deductedAmount = deductedAmount;
        result.remainingBalance = remainingBalance;
        return result;
    }
    
    public static DeductResult fail(String message) {
        DeductResult result = new DeductResult();
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
    
    public long getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }
    
    public long getDeductedAmount() {
        return deductedAmount;
    }
    
    public void setDeductedAmount(long deductedAmount) {
        this.deductedAmount = deductedAmount;
    }
    
    public long getRemainingBalance() {
        return remainingBalance;
    }
    
    public void setRemainingBalance(long remainingBalance) {
        this.remainingBalance = remainingBalance;
    }
    
    @Override
    public String toString() {
        return "DeductResult{" +
                "success=" + success +
                ", playerId=" + playerId +
                ", deductedAmount=" + deductedAmount +
                ", remainingBalance=" + remainingBalance +
                '}';
    }
}

