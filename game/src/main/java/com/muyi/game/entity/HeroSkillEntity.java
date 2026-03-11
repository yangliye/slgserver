package com.muyi.game.entity;

import com.muyi.db.annotation.Column;
import com.muyi.db.annotation.PrimaryKey;
import com.muyi.db.annotation.Table;
import com.muyi.db.core.BaseEntity;

/**
 * 英雄技能实体
 *
 * @author muyi
 */
@Table("t_hero_skill")
public class HeroSkillEntity extends BaseEntity<HeroSkillEntity> {

    @PrimaryKey(autoIncrement = true)
    private long id;

    @Column
    private long uid;

    @Column("hero_id")
    private int heroId;

    @Column("skill_id")
    private int skillId;

    @Column
    private int level;

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

    public int getSkillId() {
        return skillId;
    }

    public void setSkillId(int skillId) {
        this.skillId = skillId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
        markChanged("level");
    }

    @Override
    public String toString() {
        return "HeroSkillEntity{uid=" + uid + ", heroId=" + heroId + ", skillId=" + skillId + ", lv=" + level + "}";
    }
}
