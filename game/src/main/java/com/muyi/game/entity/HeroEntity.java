package com.muyi.game.entity;

import com.muyi.db.annotation.Column;
import com.muyi.db.annotation.PrimaryKey;
import com.muyi.db.annotation.Table;
import com.muyi.db.core.BaseEntity;

/**
 * 英雄实体
 *
 * @author muyi
 */
@Table("t_hero")
public class HeroEntity extends BaseEntity<HeroEntity> {

    @PrimaryKey(autoIncrement = true)
    private long id;

    @Column
    private long uid;

    @Column("hero_id")
    private int heroId;

    @Column
    private int level;

    @Column
    private int exp;

    @Column
    private int star;

    // ==================== Getter/Setter ====================

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public int getHeroId() {
        return heroId;
    }

    public void setHeroId(int heroId) {
        this.heroId = heroId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
        markChanged("level");
    }

    public int getExp() {
        return exp;
    }

    public void setExp(int exp) {
        this.exp = exp;
        markChanged("exp");
    }

    public int getStar() {
        return star;
    }

    public void setStar(int star) {
        this.star = star;
        markChanged("star");
    }

    @Override
    public String toString() {
        return "HeroEntity{uid=" + uid + ", heroId=" + heroId + ", lv=" + level + ", star=" + star + "}";
    }
}
