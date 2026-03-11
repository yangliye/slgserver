package com.muyi.game.manager;

import com.muyi.game.entity.HeroSkillEntity;
import com.muyi.game.playerdata.AbstractPlayerManager;
import com.muyi.game.playerdata.PlayerData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 英雄技能 Manager（玩家级）
 * <p>
 * 演示"父子数据"管理模式：通过二级索引将技能按 heroId 分组，
 * 支持按英雄维度的批量查询和级联删除。
 * <p>
 * 使用方式：
 * <pre>{@code
 * HeroSkillManager skillMgr = context.getManager(HeroSkillManager.class);
 * List<HeroSkillEntity> skills = skillMgr.getByHeroId(1001);
 * }</pre>
 * 也可从 HeroManager 中通过 getComponent 跨组件访问。
 *
 * @author muyi
 */
@PlayerData(order = 11)
public class HeroSkillManager extends AbstractPlayerManager<Long, HeroSkillEntity> {

    /** 二级索引：heroId → skills */
    private final Map<Integer, List<HeroSkillEntity>> heroIndex = new HashMap<>();

    @Override
    protected Class<HeroSkillEntity> entityClass() {
        return HeroSkillEntity.class;
    }

    @Override
    protected Long keyOf(HeroSkillEntity entity) {
        return entity.getId();
    }

    @Override
    protected void afterLoad() {
        rebuildIndex();
    }

    // ==================== 查询 ====================

    public List<HeroSkillEntity> getByHeroId(int heroId) {
        return heroIndex.getOrDefault(heroId, List.of());
    }

    // ==================== 变更 ====================

    public void addSkill(HeroSkillEntity entity) {
        add(entity);
        heroIndex.computeIfAbsent(entity.getHeroId(), k -> new ArrayList<>()).add(entity);
    }

    public void removeByHeroId(int heroId) {
        List<HeroSkillEntity> skills = heroIndex.remove(heroId);
        if (skills != null) {
            for (HeroSkillEntity skill : skills) {
                remove(skill.getId());
            }
        }
    }

    // ==================== 内部 ====================

    private void rebuildIndex() {
        heroIndex.clear();
        for (HeroSkillEntity skill : getAll()) {
            heroIndex.computeIfAbsent(skill.getHeroId(), k -> new ArrayList<>()).add(skill);
        }
    }
}
