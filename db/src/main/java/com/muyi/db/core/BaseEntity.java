package com.muyi.db.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 实体基类
 * <p>
 * 提供变更追踪、版本控制、异步落地支持
 * </p>
 *
 * @param <T> 实体类型
 */
public abstract class BaseEntity<T extends BaseEntity<T>> {

    // ==================== 元数据缓存 ====================
    
    private static final Map<Class<?>, EntityMetadata> METADATA_CACHE = new ConcurrentHashMap<>();

    // ==================== 实体状态 ====================
    
    /**
     * 实体状态
     */
    private transient volatile EntityState state = EntityState.NEW;

    /**
     * 变更的字段集合（线程安全）
     */
    private transient Set<String> changedFields;

    /**
     * 业务操作版本（每次修改递增，原子操作）
     */
    private transient final AtomicLong businessVersion = new AtomicLong(0);

    /**
     * 数据库成功版本（原子操作）
     */
    private transient final AtomicLong dbVersion = new AtomicLong(0);

    /**
     * 是否在落地队列中
     */
    private transient volatile boolean inLandQueue = false;

    /**
     * 动态表名（支持分表）
     */
    private transient String dynamicTableName;

    // ==================== 构造方法 ====================

    protected BaseEntity() {
        // 使用线程安全的 Set
        this.changedFields = ConcurrentHashMap.newKeySet();
    }

    // ==================== 变更追踪 ====================

    /**
     * 标记字段变更（子类 setter 中调用）
     * <p>
     * 线程安全：使用 ConcurrentHashMap.KeySet 和 AtomicLong
     */
    protected void markChanged(String fieldName) {
        if (changedFields != null) {
            changedFields.add(fieldName);
            businessVersion.incrementAndGet();
        }
    }

    /**
     * 获取变更的字段
     */
    public Set<String> getChangedFields() {
        return changedFields != null ? Collections.unmodifiableSet(changedFields) : Collections.emptySet();
    }

    /**
     * 清除变更标记
     */
    public void clearChanges() {
        if (changedFields != null) {
            changedFields.clear();
        }
    }

    /**
     * 是否有变更
     */
    public boolean hasChanges() {
        return changedFields != null && !changedFields.isEmpty();
    }

    /**
     * 是否需要落地
     */
    public boolean needLand() {
        return businessVersion.get() > dbVersion.get();
    }

    // ==================== 状态管理 ====================

    public EntityState getState() {
        return state;
    }

    public void setState(EntityState state) {
        this.state = state;
    }

    public long getBusinessVersion() {
        return businessVersion.get();
    }

    public long getDbVersion() {
        return dbVersion.get();
    }

    public void setDbVersion(long dbVersion) {
        this.dbVersion.set(dbVersion);
    }

    /**
     * 同步版本号（将 dbVersion 更新到当前 businessVersion）
     * <p>
     * 注意：此操作非原子，但在单线程落地场景下是安全的。
     * 如果需要在多线程环境下使用，请确保外部同步。
     */
    public void syncVersion() {
        // 获取当前业务版本并设置到 DB 版本
        // 这里不需要原子操作，因为：
        // 1. 落地成功后调用，此时实体不会被其他线程修改
        // 2. 即使有修改，下次落地会再次同步
        this.dbVersion.set(this.businessVersion.get());
    }
    
    /**
     * 同步版本号到指定值
     * 
     * @param version 要同步到的版本号
     */
    public void syncVersionTo(long version) {
        this.dbVersion.set(version);
    }

    public boolean isInLandQueue() {
        return inLandQueue;
    }

    public void setInLandQueue(boolean inLandQueue) {
        this.inLandQueue = inLandQueue;
    }

    // ==================== 表名 ====================

    /**
     * 获取表名
     */
    public String getTableName() {
        if (dynamicTableName != null) {
            return dynamicTableName;
        }
        return getMetadata().getTableName();
    }

    /**
     * 设置动态表名（用于分表）
     */
    public void setDynamicTableName(String tableName) {
        this.dynamicTableName = tableName;
    }

    // ==================== 元数据 ====================

    /**
     * 获取实体元数据
     */
    public EntityMetadata getMetadata() {
        return METADATA_CACHE.computeIfAbsent(this.getClass(), EntityMetadata::new);
    }

    /**
     * 获取主键值
     */
    public Object[] getPrimaryKeyValues() {
        return getMetadata().getPrimaryKeyValues(this);
    }

    /**
     * 获取所有字段值（按顺序）
     */
    public Object[] getAllValues() {
        return getMetadata().getAllValues(this);
    }

    /**
     * 从 Map 初始化实体
     */
    public void fromMap(Map<String, Object> values) {
        getMetadata().setValues(this, values);
        this.state = EntityState.PERSISTENT;
        this.clearChanges();
        this.dbVersion.set(this.businessVersion.get());
    }

    /**
     * 转换为 Map
     */
    public Map<String, Object> toMap() {
        return getMetadata().toMap(this);
    }

    // ==================== 实体复制 ====================

    /**
     * 创建新实例
     */
    @SuppressWarnings("unchecked")
    public T newInstance() {
        try {
            T instance = (T) this.getClass().getDeclaredConstructor().newInstance();
            instance.setDynamicTableName(this.dynamicTableName);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create new instance", e);
        }
    }

    // ==================== Object 方法 ====================

    @Override
    public String toString() {
        return getClass().getSimpleName() + toMap();
    }

    @Override
    public int hashCode() {
        Object[] pkValues = getPrimaryKeyValues();
        return Arrays.hashCode(pkValues);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BaseEntity<?> other = (BaseEntity<?>) obj;
        return Arrays.equals(getPrimaryKeyValues(), other.getPrimaryKeyValues());
    }
}
