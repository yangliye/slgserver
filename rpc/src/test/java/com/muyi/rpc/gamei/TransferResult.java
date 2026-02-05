package com.muyi.rpc.gamei;

import java.io.Serializable;

/**
 * 跨服转账结果
 *
 * @author muyi
 */
public class TransferResult implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private boolean success;
    private String message;
    private long fromPlayerId;
    private long toPlayerId;
    private long amount;
    private long fromPlayerBalance;
    private long toPlayerBalance;
    
    public static TransferResult success(long fromPlayerId, long toPlayerId, long amount,
                                         long fromBalance, long toBalance) {
        TransferResult result = new TransferResult();
        result.success = true;
        result.message = "Transfer successful";
        result.fromPlayerId = fromPlayerId;
        result.toPlayerId = toPlayerId;
        result.amount = amount;
        result.fromPlayerBalance = fromBalance;
        result.toPlayerBalance = toBalance;
        return result;
    }
    
    public static TransferResult fail(String message) {
        TransferResult result = new TransferResult();
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
    
    public long getFromPlayerId() {
        return fromPlayerId;
    }
    
    public void setFromPlayerId(long fromPlayerId) {
        this.fromPlayerId = fromPlayerId;
    }
    
    public long getToPlayerId() {
        return toPlayerId;
    }
    
    public void setToPlayerId(long toPlayerId) {
        this.toPlayerId = toPlayerId;
    }
    
    public long getAmount() {
        return amount;
    }
    
    public void setAmount(long amount) {
        this.amount = amount;
    }
    
    public long getFromPlayerBalance() {
        return fromPlayerBalance;
    }
    
    public void setFromPlayerBalance(long fromPlayerBalance) {
        this.fromPlayerBalance = fromPlayerBalance;
    }
    
    public long getToPlayerBalance() {
        return toPlayerBalance;
    }
    
    public void setToPlayerBalance(long toPlayerBalance) {
        this.toPlayerBalance = toPlayerBalance;
    }
    
    @Override
    public String toString() {
        return "TransferResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", amount=" + amount +
                ", fromPlayerId=" + fromPlayerId +
                ", toPlayerId=" + toPlayerId +
                '}';
    }
}

