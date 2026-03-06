package com.muyi.game.manager;

import com.muyi.game.playerdata.AbstractPlayerManager;
import com.muyi.game.playerdata.PlayerData;
import com.muyi.game.entity.HeroEntity;

import java.util.Collection;

/**
 * 英雄 Manager（玩家级）
 * <p>
 * 管理单个玩家的所有英雄数据，提供增删改查和业务方法。
 * <p>
 * 使用方式：
 * <pre>{@code
 * HeroManager heroMgr = context.getManager(HeroManager.class);
 * HeroEntity hero = heroMgr.getByHeroId(1001);
 * heroMgr.addExp(1001, 500);
 * }</pre>
 *
 * @author muyi
 */
@PlayerData(order = 10)
public class HeroManager extends AbstractPlayerManager<HeroEntity> {

    @Override
    protected Class<HeroEntity> entityClass() {
        return HeroEntity.class;
    }

    @Override
    protected int keyOf(HeroEntity entity) {
        return entity.getHeroId();
    }

    // ==================== 查询 ====================

    public HeroEntity getByHeroId(int heroId) {
        return get(heroId);
    }

    public Collection<HeroEntity> getAllHeroes() {
        return getAll();
    }

    public int getHeroCount() {
        return size();
    }

    public boolean hasHero(int heroId) {
        return contains(heroId);
    }

    // ==================== 业务方法 ====================
}
