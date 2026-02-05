package com.muyi.db.example;

import com.muyi.db.annotation.Column;
import com.muyi.db.annotation.PrimaryKey;
import com.muyi.db.annotation.Table;
import com.muyi.db.core.BaseEntity;

/**
 * 示例: 玩家建筑实体（一对多关系）
 */
@Table(value = "player_building", shardKey = "uid")
public class PlayerBuildingEntity extends BaseEntity<PlayerBuildingEntity> {

    @PrimaryKey
    @Column("uuid")
    private long uuid;

    @Column("uid")
    private long uid;

    @Column("config_id")
    private int configId;

    @Column("level")
    private int level;

    @Column("x")
    private int x;

    @Column("y")
    private int y;

    @Column("status")
    private int status;

    public PlayerBuildingEntity() {
    }

    public PlayerBuildingEntity(long uid) {
        this.uid = uid;
    }

    // ==================== Getter/Setter ====================

    public long getUuid() {
        return uuid;
    }

    public void setUuid(long uuid) {
        this.uuid = uuid;
        markChanged("uuid");
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
        markChanged("uid");
    }

    public int getConfigId() {
        return configId;
    }

    public void setConfigId(int configId) {
        this.configId = configId;
        markChanged("configId");
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
        markChanged("level");
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
        markChanged("x");
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
        markChanged("y");
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
        markChanged("status");
    }
}
