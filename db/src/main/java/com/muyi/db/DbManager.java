package com.muyi.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.muyi.db.async.AsyncLandConfig;
import com.muyi.db.async.AsyncLandManager;
import com.muyi.db.config.DbConfig;
import com.muyi.db.core.BaseEntity;
import com.muyi.db.sql.SqlExecutor;

/**
 * 数据库管理器
 * <p>
 * 统一入口，提供:
 * <ul>
 *   <li>同步 CRUD 操作</li>
 *   <li>异步落地</li>
 *   <li>批量操作</li>
 *   <li>事务支持</li>
 * </ul>
 *
 * <h3>使用示例:</h3>
 * <pre>{@code
 * // 1. 初始化
 * DbConfig config = new DbConfig()
 *     .jdbcUrl("jdbc:mysql://localhost:3306/game")
 *     .username("root")
 *     .password("123456");
 * DbManager db = new DbManager(config);
 *
 * // 2. 同步操作
 * PlayerEntity player = new PlayerEntity();
 * player.setUid(10001L);
 * player.setName("test");
 * db.insert(player);
 *
 * // 3. 异步落地
 * player.setLevel(10);
 * db.submitUpdate(player);  // 异步执行
 *
 * // 4. 关闭
 * db.shutdown();
 * }</pre>
 */
public class DbManager {

    private static final Logger logger = LoggerFactory.getLogger(DbManager.class);

    private final DbConfig config;
    private final DataSource dataSource;
    private final SqlExecutor sqlExecutor;
    private final AsyncLandManager asyncLandManager;
    
    /**
     * 关闭状态标志
     */
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public DbManager(DbConfig config) {
        this(config, config.createDataSource());
    }

    public DbManager(DbConfig config, DataSource dataSource) {
        this.config = config;
        this.dataSource = dataSource;
        this.sqlExecutor = new SqlExecutor(dataSource);
        this.asyncLandManager = new AsyncLandManager(sqlExecutor,
                new AsyncLandConfig()
                        .landThreads(config.getLandThreads())
                        .landIntervalMs(config.getLandIntervalMs())
                        .batchSize(config.getLandBatchSize())
                        .maxRetries(config.getLandMaxRetries())
        );

        logger.info("DbManager initialized");
    }

    // ==================== 同步操作 ====================

    /**
     * 同步插入
     */
    public <T extends BaseEntity<T>> boolean insert(T entity) {
        checkNotShutdown();
        return sqlExecutor.insert(entity);
    }

    /**
     * 同步插入（自增主键回填）
     */
    public <T extends BaseEntity<T>> boolean insertWithKey(T entity, String keyField) {
        checkNotShutdown();
        return sqlExecutor.insertWithGeneratedKey(entity, keyField);
    }

    /**
     * 同步更新（全量）
     */
    public <T extends BaseEntity<T>> boolean update(T entity) {
        checkNotShutdown();
        return sqlExecutor.update(entity);
    }

    /**
     * 同步更新（只更新变更字段）
     */
    public <T extends BaseEntity<T>> boolean updatePartial(T entity) {
        checkNotShutdown();
        return sqlExecutor.updatePartial(entity);
    }

    /**
     * 同步插入或更新
     */
    public <T extends BaseEntity<T>> boolean upsert(T entity) {
        checkNotShutdown();
        return sqlExecutor.upsert(entity);
    }

    /**
     * 同步删除
     */
    public <T extends BaseEntity<T>> boolean delete(T entity) {
        checkNotShutdown();
        return sqlExecutor.delete(entity);
    }

    // ==================== 查询操作（自动合并脏数据）====================

    /**
     * 按主键查询（优先返回脏数据）
     * <p>
     * 查询顺序：
     * 1. 先检查脏数据缓存中是否有该实体
     * 2. 如果脏数据中标记为已删除，返回 null
     * 3. 如果脏数据存在，直接返回
     * 4. 否则查询数据库
     */
    @SuppressWarnings("unchecked")
    public <T extends BaseEntity<T>> T selectByPrimaryKey(T template, Object... pkValues) {
        Class<T> entityClass = (Class<T>) template.getClass();
        Object cacheKey = pkValues.length == 1 ? pkValues[0] : java.util.Arrays.asList(pkValues);
        
        // 检查是否已被标记删除
        if (asyncLandManager.isDeleted(entityClass, cacheKey)) {
            return null;
        }
        
        // 检查脏数据缓存
        T dirty = asyncLandManager.getDirty(entityClass, cacheKey);
        if (dirty != null) {
            return dirty;
        }
        
        // 查询数据库
        return sqlExecutor.selectByPrimaryKey(template, pkValues);
    }

    /**
     * 按条件查询（自动合并脏数据）
     * <p>
     * 合并逻辑：
     * 1. 查询数据库获取结果
     * 2. 获取该类型所有脏数据
     * 3. 用脏数据覆盖 DB 结果（按主键匹配）
     * 4. 脏数据中符合条件但 DB 没有的，补充到结果
     * 5. 排除已删除的实体
     * 
     * @param template 模板实体
     * @param conditions 查询条件
     * @param conditionMatcher 条件匹配器（判断脏数据是否符合条件），null 则不补充新数据
     */
    @SuppressWarnings("unchecked")
    public <T extends BaseEntity<T>> List<T> selectByCondition(T template, Map<String, Object> conditions, 
            Predicate<T> conditionMatcher) {
        Class<T> entityClass = (Class<T>) template.getClass();
        
        // 查询数据库
        List<T> dbResults = sqlExecutor.selectByCondition(template, conditions);
        
        // 获取脏数据
        List<T> dirtyList = asyncLandManager.getAllDirty(entityClass);
        if (dirtyList.isEmpty()) {
            // 没有脏数据，只需排除已删除的
            return filterDeleted(dbResults, entityClass);
        }
        
        // 构建主键到脏数据的映射
        Map<Object, T> dirtyMap = new HashMap<>();
        for (T dirty : dirtyList) {
            Object pk = createPrimaryKey(dirty);
            dirtyMap.put(pk, dirty);
        }
        
        // 合并结果
        List<T> merged = new ArrayList<>();
        for (T dbEntity : dbResults) {
            Object pk = createPrimaryKey(dbEntity);
            
            // 检查是否已删除
            if (asyncLandManager.isDeleted(entityClass, pk)) {
                continue;
            }
            
            // 用脏数据覆盖
            T dirty = dirtyMap.remove(pk);
            merged.add(dirty != null ? dirty : dbEntity);
        }
        
        // 补充脏数据中有但 DB 没有的（新增的）
        if (conditionMatcher != null) {
            for (T dirty : dirtyMap.values()) {
                if (conditionMatcher.test(dirty)) {
                    merged.add(dirty);
                }
            }
        }
        
        return merged;
    }
    
    /**
     * 按条件查询（简化版，不补充新数据）
     */
    public <T extends BaseEntity<T>> List<T> selectByCondition(T template, Map<String, Object> conditions) {
        return selectByCondition(template, conditions, null);
    }

    /**
     * 执行自定义查询（自动排除已删除的脏数据）
     * <p>
     * 注意：自定义 SQL 无法自动补充脏数据中的新增数据，
     * 如需完整合并，请使用 selectByCondition 或手动处理
     */
    @SuppressWarnings("unchecked")
    public <T extends BaseEntity<T>> List<T> selectBySql(T template, String sql, Object... params) {
        Class<T> entityClass = (Class<T>) template.getClass();
        List<T> results = sqlExecutor.selectBySql(template, sql, params);
        return filterDeleted(results, entityClass);
    }

    /**
     * 执行自定义查询（返回 Map）
     */
    public List<Map<String, Object>> selectMapBySql(String sql, Object... params) {
        return sqlExecutor.selectMapBySql(sql, params);
    }

    /**
     * 查询单个值（用于 COUNT、SUM、MAX 等聚合查询）
     * <p>
     * 示例：
     * <pre>{@code
     * long count = db.selectOne("SELECT COUNT(*) FROM player WHERE level > ?", Long.class, 0L, 10);
     * Long sum = db.selectOne("SELECT SUM(gold) FROM player", Long.class, null);
     * }</pre>
     *
     * @param sql SQL 语句
     * @param resultType 结果类型
     * @param defaultValue 默认值（查询无结果或结果为 null 时返回）
     * @param params SQL 参数
     */
    public <T> T selectOne(String sql, Class<T> resultType, T defaultValue, Object... params) {
        return sqlExecutor.selectOne(sql, resultType, defaultValue, params);
    }
    
    // ==================== 脏数据辅助方法 ====================
    
    /**
     * 从脏数据缓存获取实体
     */
    public <T extends BaseEntity<T>> T getDirty(Class<T> entityClass, Object primaryKey) {
        return asyncLandManager.getDirty(entityClass, primaryKey);
    }
    
    /**
     * 获取某类型所有脏数据
     */
    public <T extends BaseEntity<T>> List<T> getAllDirty(Class<T> entityClass) {
        return asyncLandManager.getAllDirty(entityClass);
    }
    
    /**
     * 检查实体是否在脏数据中被标记删除
     */
    public boolean isDirtyDeleted(Class<?> entityClass, Object primaryKey) {
        return asyncLandManager.isDeleted(entityClass, primaryKey);
    }
    
    private Object createPrimaryKey(BaseEntity<?> entity) {
        Object[] pkValues = entity.getPrimaryKeyValues();
        return pkValues.length == 1 ? pkValues[0] : java.util.Arrays.asList(pkValues);
    }
    
    private <T extends BaseEntity<T>> List<T> filterDeleted(List<T> results, Class<T> entityClass) {
        if (results.isEmpty()) {
            return results;
        }
        
        List<T> filtered = new ArrayList<>(results.size());
        for (T entity : results) {
            Object pk = createPrimaryKey(entity);
            if (!asyncLandManager.isDeleted(entityClass, pk)) {
                // 检查是否有脏数据覆盖
                T dirty = asyncLandManager.getDirty(entityClass, pk);
                filtered.add(dirty != null ? dirty : entity);
            }
        }
        return filtered;
    }

    // ==================== 批量操作 ====================

    /**
     * 批量插入
     */
    public <T extends BaseEntity<T>> int[] batchInsert(List<T> entities) {
        checkNotShutdown();
        return sqlExecutor.batchInsert(entities);
    }

    /**
     * 批量更新
     */
    public <T extends BaseEntity<T>> int[] batchUpdate(List<T> entities) {
        checkNotShutdown();
        return sqlExecutor.batchUpdate(entities);
    }

    /**
     * 批量删除
     */
    public <T extends BaseEntity<T>> int[] batchDelete(List<T> entities) {
        checkNotShutdown();
        return sqlExecutor.batchDelete(entities);
    }

    // ==================== 异步落地 ====================

    /**
     * 提交异步插入
     */
    public void submitInsert(BaseEntity<?> entity) {
        checkNotShutdown();
        asyncLandManager.submitInsert(entity);
    }

    /**
     * 提交异步更新
     */
    public void submitUpdate(BaseEntity<?> entity) {
        checkNotShutdown();
        asyncLandManager.submitUpdate(entity);
    }

    /**
     * 提交异步删除
     */
    public void submitDelete(BaseEntity<?> entity) {
        checkNotShutdown();
        asyncLandManager.submitDelete(entity);
    }

    /**
     * 立即同步落地
     */
    public boolean landNow(BaseEntity<?> entity) {
        checkNotShutdown();
        return asyncLandManager.landNow(entity);
    }

    // ==================== 事务 ====================

    /**
     * 执行事务
     */
    public void executeInTransaction(java.util.function.Consumer<java.sql.Connection> action) {
        checkNotShutdown();
        sqlExecutor.executeInTransaction(action);
    }

    // ==================== 原生 SQL ====================

    /**
     * 执行原生 SQL（更新）
     */
    public int executeSql(String sql, Object... params) {
        checkNotShutdown();
        return sqlExecutor.executeSql(sql, params);
    }

    // ==================== 生命周期 ====================

    /**
     * 关闭（会等待所有异步任务完成）
     */
    public void shutdown() {
        if (!shutdown.compareAndSet(false, true)) {
            logger.warn("DbManager already shutdown");
            return;
        }
        
        logger.info("Shutting down DbManager...");
        asyncLandManager.shutdown();
        
        if (dataSource instanceof AutoCloseable) {
            try {
                ((AutoCloseable) dataSource).close();
            } catch (Exception e) {
                logger.error("Failed to close datasource", e);
            }
        }
        
        logger.info("DbManager shutdown completed");
    }
    
    /**
     * 是否已关闭
     */
    public boolean isShutdown() {
        return shutdown.get();
    }
    
    /**
     * 检查是否已关闭，如果已关闭则抛出异常
     */
    private void checkNotShutdown() {
        if (shutdown.get()) {
            throw new IllegalStateException("DbManager has been shutdown");
        }
    }

    // ==================== Getter ====================

    public DbConfig getConfig() {
        return config;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public SqlExecutor getSqlExecutor() {
        return sqlExecutor;
    }

    public AsyncLandManager getAsyncLandManager() {
        return asyncLandManager;
    }

    // ==================== 统计 ====================

    public long getTotalLandTasks() {
        return asyncLandManager.getTotalTasks();
    }

    public long getSuccessLandTasks() {
        return asyncLandManager.getSuccessTasks();
    }

    public long getFailedLandTasks() {
        return asyncLandManager.getFailedTasks();
    }
    
    /**
     * 获取重试次数（监控用）
     */
    public long getLandRetryCount() {
        return asyncLandManager.getRetryCount();
    }

    public int getPendingLandTasks() {
        return asyncLandManager.getPendingTasks();
    }

    // ==================== 动态配置调整 ====================

    /**
     * 动态调整批量大小
     * <p>
     * 使用场景：
     * <ul>
     *   <li>大战期间：增大批量（300-500）提高吞吐量</li>
     *   <li>低峰期：减小批量（50-100）降低延迟</li>
     * </ul>
     * 
     * @param batchSize 新的批量大小
     */
    public void setLandBatchSize(int batchSize) {
        asyncLandManager.setBatchSize(batchSize);
    }

    /**
     * 动态调整落地间隔
     * <p>
     * 使用场景：
     * <ul>
     *   <li>大战期间：缩短间隔（500-1000ms）更快落地，降低数据丢失风险</li>
     *   <li>资源产出：延长间隔（2000-3000ms）充分合并，提高批量效率</li>
     * </ul>
     * 
     * @param landIntervalMs 新的落地间隔（毫秒）
     */
    public void setLandIntervalMs(long landIntervalMs) {
        asyncLandManager.setLandIntervalMs(landIntervalMs);
    }
    
    /**
     * 启用/禁用自适应调整
     * <p>
     * 自适应调整会根据每个工作线程的队列长度自动调整 batchSize 和 landIntervalMs：
     * <ul>
     *   <li>队列积压时：缩短间隔、增大批量，加快处理</li>
     *   <li>队列空闲时：延长间隔、减小批量，充分合并</li>
     * </ul>
     * 
     * @param enabled 是否启用
     */
    public void setAdaptiveEnabled(boolean enabled) {
        asyncLandManager.setAdaptiveEnabled(enabled);
    }
    
    /**
     * 设置队列积压阈值
     * <p>
     * 当工作线程队列长度超过此值时，触发自适应调整（缩短间隔、增大批量）
     * 
     * @param threshold 积压阈值
     */
    public void setBacklogThreshold(int threshold) {
        asyncLandManager.setBacklogThreshold(threshold);
    }
    
    /**
     * 设置队列空闲阈值
     * <p>
     * 当工作线程队列长度低于此值时，触发自适应调整（延长间隔、减小批量）
     * 
     * @param threshold 空闲阈值
     */
    public void setIdleThreshold(int threshold) {
        asyncLandManager.setIdleThreshold(threshold);
    }
}
