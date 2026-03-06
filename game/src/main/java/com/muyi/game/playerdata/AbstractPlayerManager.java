package com.muyi.game.playerdata;

import com.muyi.db.DbManager;
import com.muyi.db.core.BaseEntity;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家数据 Manager 基类（有 DB 实体）
 * <p>
 * 每个在线玩家持有一组 Manager 实例，每个 Manager 管理一种类型的数据。
 * 子类只需要实现 {@link #entityClass()} 和 {@link #keyOf(BaseEntity)}，
 * 框架自动完成数据加载、缓存、增删改的落库。
 * <p>
 * 使用示例:
 * <pre>{@code
 * @PlayerData(order = 10)
 * public class HeroManager extends AbstractPlayerManager<Integer, HeroEntity> {
 *
 *     @Override
 *     protected Class<HeroEntity> entityClass() { return HeroEntity.class; }
 *
 *     @Override
 *     protected Integer keyOf(HeroEntity entity) { return entity.getHeroId(); }
 *
 *     public HeroEntity getHero(int heroId) { return get(heroId); }
 * }
 * }</pre>
 *
 * @param <K> 业务 key 类型（Integer, Long, String 等）
 * @param <T> 实体类型
 * @author muyi
 * @see AbstractPlayerLogic
 */
public abstract class AbstractPlayerManager<K, T extends BaseEntity<T>> extends AbstractPlayerComponent {

    private DbManager db;
    private final Map<K, T> dataMap = new ConcurrentHashMap<>();

    // ==================== 子类必须实现 ====================

    /**
     * 实体类型
     */
    protected abstract Class<T> entityClass();

    /**
     * 从实体中提取业务 key（用于内存索引）
     * <p>
     * 对于单条数据的 Manager（如玩家基础信息），直接返回固定值即可
     */
    protected abstract K keyOf(T entity);

    // ==================== 子类可选重写 ====================

    /**
     * 加载条件列名，默认按 uid 加载
     */
    protected String loadColumn() {
        return "uid";
    }

    /**
     * 数据加载完成后的回调（可用于构建索引、计算派生数据等）
     */
    protected void afterLoad() {
    }

    // ==================== 框架调用（包级可见）====================

    @Override
    void initComponent(long uid, PlayerDataContext context) {
        super.initComponent(uid, context);
        this.db = context.db();
    }

    @Override
    void load() {
        T template = newEntity();
        List<T> entities = db.selectByCondition(template, Map.of(loadColumn(), getUid()));
        for (T entity : entities) {
            dataMap.put(keyOf(entity), entity);
        }
        afterLoad();
        log.debug("Player[{}] loaded {} {} records", getUid(), dataMap.size(), entityClass().getSimpleName());
    }

    @Override
    void clear() {
        super.clear();
        dataMap.clear();
    }

    // ==================== 数据访问 ====================

    protected T get(K key) {
        return dataMap.get(key);
    }

    protected Collection<T> getAll() {
        return Collections.unmodifiableCollection(dataMap.values());
    }

    protected int size() {
        return dataMap.size();
    }

    protected boolean contains(K key) {
        return dataMap.containsKey(key);
    }

    // ==================== 数据变更（自动落库）====================

    /**
     * 新增数据（内存 + 异步入库）
     */
    protected void add(T entity) {
        dataMap.put(keyOf(entity), entity);
        db.submitInsert(entity);
    }

    /**
     * 提交异步更新（entity 的字段变更应在 setter 中通过 markChanged 完成）
     */
    protected void update(T entity) {
        db.submitUpdate(entity);
    }

    /**
     * 删除数据（内存移除 + 异步入库）
     */
    protected void remove(K key) {
        T entity = dataMap.remove(key);
        if (entity != null) {
            db.submitDelete(entity);
        }
    }

    /**
     * 同步插入（阻塞等待落库完成，用于需要立即获取自增 ID 的场景）
     */
    protected void addSync(T entity) {
        db.insert(entity);
        dataMap.put(keyOf(entity), entity);
    }

    // ==================== 工具方法 ====================

    protected DbManager getDb() {
        return db;
    }

    private T newEntity() {
        try {
            return entityClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Cannot create entity: " + entityClass().getName(), e);
        }
    }
}
