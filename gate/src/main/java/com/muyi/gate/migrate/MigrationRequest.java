package com.muyi.gate.migrate;

import java.io.Serializable;

/**
 * 迁服请求
 * 
 * 支持三种迁移类型：
 * - WORLD：只切换 World 服务器（最常见，如跨区域）
 * - GAME：只切换 Game 服务器（少见）
 * - FULL：Game 和 World 都切换（如合服）
 *
 * @author muyi
 */
public class MigrationRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 玩家ID */
    private long playerId;
    
    /** 迁移类型 */
    private MigrationType type = MigrationType.WORLD;
    
    // ========== World 迁移信息 ==========
    
    /** 源 World 服务器ID */
    private int sourceWorldServerId;
    
    /** 目标 World 服务器ID */
    private int targetWorldServerId;
    
    /** 目标 World 服务器地址 */
    private String targetWorldServerAddress;
    
    // ========== Game 迁移信息（仅 GAME/FULL 类型需要）==========
    
    /** 源 Game 服务器ID */
    private int sourceGameServerId;
    
    /** 目标 Game 服务器ID */
    private int targetGameServerId;
    
    /** 目标 Game 服务器地址 */
    private String targetGameServerAddress;
    
    // ========== 通用信息 ==========
    
    /** 迁服原因 */
    private String reason;
    
    /** 是否强制迁服 */
    private boolean force;
    
    public MigrationRequest() {
    }
    
    /**
     * 创建 World 迁移请求（最常用）
     */
    public static MigrationRequest worldMigration(long playerId, 
                                                   int sourceWorldServerId, 
                                                   int targetWorldServerId,
                                                   String targetWorldServerAddress,
                                                   String reason) {
        MigrationRequest request = new MigrationRequest();
        request.playerId = playerId;
        request.type = MigrationType.WORLD;
        request.sourceWorldServerId = sourceWorldServerId;
        request.targetWorldServerId = targetWorldServerId;
        request.targetWorldServerAddress = targetWorldServerAddress;
        request.reason = reason;
        return request;
    }
    
    /**
     * 创建完整迁移请求（Game + World）
     */
    public static MigrationRequest fullMigration(long playerId,
                                                  int sourceGameServerId, int targetGameServerId, String targetGameAddress,
                                                  int sourceWorldServerId, int targetWorldServerId, String targetWorldAddress,
                                                  String reason) {
        MigrationRequest request = new MigrationRequest();
        request.playerId = playerId;
        request.type = MigrationType.FULL;
        request.sourceGameServerId = sourceGameServerId;
        request.targetGameServerId = targetGameServerId;
        request.targetGameServerAddress = targetGameAddress;
        request.sourceWorldServerId = sourceWorldServerId;
        request.targetWorldServerId = targetWorldServerId;
        request.targetWorldServerAddress = targetWorldAddress;
        request.reason = reason;
        return request;
    }
    
    /**
     * @deprecated 使用 worldMigration() 或 fullMigration() 静态方法
     */
    @Deprecated
    public MigrationRequest(long playerId, int sourceServerId, int targetServerId, 
                            String targetServerAddress, String reason) {
        this.playerId = playerId;
        this.type = MigrationType.WORLD;
        this.sourceWorldServerId = sourceServerId;
        this.targetWorldServerId = targetServerId;
        this.targetWorldServerAddress = targetServerAddress;
        this.reason = reason;
        this.force = false;
    }
    
    // ========== Getter/Setter ==========
    
    public long getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }
    
    public MigrationType getType() {
        return type;
    }
    
    public void setType(MigrationType type) {
        this.type = type;
    }
    
    public int getSourceWorldServerId() {
        return sourceWorldServerId;
    }
    
    public void setSourceWorldServerId(int sourceWorldServerId) {
        this.sourceWorldServerId = sourceWorldServerId;
    }
    
    public int getTargetWorldServerId() {
        return targetWorldServerId;
    }
    
    public void setTargetWorldServerId(int targetWorldServerId) {
        this.targetWorldServerId = targetWorldServerId;
    }
    
    public String getTargetWorldServerAddress() {
        return targetWorldServerAddress;
    }
    
    public void setTargetWorldServerAddress(String targetWorldServerAddress) {
        this.targetWorldServerAddress = targetWorldServerAddress;
    }
    
    public int getSourceGameServerId() {
        return sourceGameServerId;
    }
    
    public void setSourceGameServerId(int sourceGameServerId) {
        this.sourceGameServerId = sourceGameServerId;
    }
    
    public int getTargetGameServerId() {
        return targetGameServerId;
    }
    
    public void setTargetGameServerId(int targetGameServerId) {
        this.targetGameServerId = targetGameServerId;
    }
    
    public String getTargetGameServerAddress() {
        return targetGameServerAddress;
    }
    
    public void setTargetGameServerAddress(String targetGameServerAddress) {
        this.targetGameServerAddress = targetGameServerAddress;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public boolean isForce() {
        return force;
    }
    
    public void setForce(boolean force) {
        this.force = force;
    }
    
    /**
     * @deprecated 使用 getSourceWorldServerId()
     */
    @Deprecated
    public int getSourceServerId() {
        return sourceWorldServerId;
    }
    
    /**
     * @deprecated 使用 getTargetWorldServerId()
     */
    @Deprecated
    public int getTargetServerId() {
        return targetWorldServerId;
    }
    
    /**
     * @deprecated 使用 getTargetWorldServerAddress()
     */
    @Deprecated
    public String getTargetServerAddress() {
        return targetWorldServerAddress;
    }
    
    @Override
    public String toString() {
        return "MigrationRequest{" +
                "playerId=" + playerId +
                ", type=" + type +
                ", sourceWorld=" + sourceWorldServerId +
                ", targetWorld=" + targetWorldServerId +
                ", sourceGame=" + sourceGameServerId +
                ", targetGame=" + targetGameServerId +
                ", reason='" + reason + '\'' +
                '}';
    }
}
