package com.muyi.db.example;

import com.muyi.db.annotation.Column;
import com.muyi.db.annotation.PrimaryKey;
import com.muyi.db.annotation.Table;
import com.muyi.db.core.BaseEntity;

/**
 * 示例: 玩家实体
 */
@Table("player")
public class PlayerEntity extends BaseEntity<PlayerEntity> {

    @PrimaryKey
    @Column("uid")
    private long uid;

    @Column("name")
    private String name;

    @Column("level")
    private int level;

    @Column("exp")
    private long exp;

    @Column("vip_level")
    private int vipLevel;

    @Column("server_id")
    private int serverId;

    @Column("create_time")
    private long createTime;

    @Column("last_login_time")
    private long lastLoginTime;

    public PlayerEntity() {
    }

    public PlayerEntity(long uid) {
        this.uid = uid;
    }

    // ==================== Getter/Setter（带变更追踪）====================

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
        markChanged("uid");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        markChanged("name");
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
        markChanged("level");
    }

    public long getExp() {
        return exp;
    }

    public void setExp(long exp) {
        this.exp = exp;
        markChanged("exp");
    }

    public int getVipLevel() {
        return vipLevel;
    }

    public void setVipLevel(int vipLevel) {
        this.vipLevel = vipLevel;
        markChanged("vipLevel");
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
        markChanged("serverId");
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
        markChanged("createTime");
    }

    public long getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(long lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
        markChanged("lastLoginTime");
    }
}
