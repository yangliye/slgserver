package com.muyi.db.async;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.muyi.db.annotation.WorkerIndex;
import com.muyi.db.core.BaseEntity;
import com.muyi.db.core.EntityState;
import com.muyi.db.sql.SqlExecutor;

/**
 * 异步落地管理器
 * <p>
 * 核心特性:
 * <ul>
 *   <li>异步批量落地，减少数据库压力</li>
 *   <li>版本控制，防止旧数据覆盖新数据</li>
 *   <li>失败重试机制</li>
 *   <li>优雅关闭，确保数据不丢失</li>
 *   <li>脏数据缓存，支持未落地数据的查询</li>
 * </ul>
 */
public class AsyncLandManager {

    private static final Logger logger = LoggerFactory.getLogger(AsyncLandManager.class);

    /**
     * 工作线程组（每个线程有独立的队列）
     * <p>
     * 按表/分片固定分配到同一个线程，保证顺序且无需锁
     */
    private final WorkerThread[] workerThreads;

    /**
     * 脏数据缓存：entityClass -> primaryKey -> entity
     * <p>
     * 存储所有待落地的实体，用于查询时返回最新数据
     */
    private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Object, BaseEntity<?>>> dirtyCache = new ConcurrentHashMap<>();
    
    /**
     * 注解解析缓存：entityClass -> workerIndex（-1 表示无注解）
     */
    private final ConcurrentHashMap<Class<?>, Integer> annotationCache = new ConcurrentHashMap<>();


    /**
     * SQL 执行器
     */
    private final SqlExecutor sqlExecutor;

    /**
     * 配置
     */
    private final AsyncLandConfig config;

    /**
     * 是否已关闭
     */
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    /**
     * 统计
     */
    private final AtomicLong totalTasks = new AtomicLong(0);
    private final AtomicLong successTasks = new AtomicLong(0);
    private final AtomicLong failedTasks = new AtomicLong(0);      // 最终失败（放弃）的任务数
    private final AtomicLong retryCount = new AtomicLong(0);       // 重试次数（用于监控）

    public AsyncLandManager(SqlExecutor sqlExecutor, AsyncLandConfig config) {
        this.sqlExecutor = sqlExecutor;
        this.config = config;
        
        // 创建工作线程组（使用 BlockingQueue，无需调度线程）
        this.workerThreads = new WorkerThread[config.getLandThreads()];
        for (int i = 0; i < config.getLandThreads(); i++) {
            workerThreads[i] = new WorkerThread(
                    i, 
                    config.getLandIntervalMs(), 
                    config.getBatchSize(),
                    this::processBatch
            );
            workerThreads[i].start();
        }

        logger.info("AsyncLandManager started with {} worker threads",
                config.getLandThreads());
    }

    // ==================== 提交任务 ====================

    /**
     * 提交插入任务
     */
    public void submitInsert(BaseEntity<?> entity) {
        entity.setState(EntityState.NEW);
        submit(LandTask.ofInsert(entity), false);
    }

    /**
     * 提交更新任务
     */
    public void submitUpdate(BaseEntity<?> entity) {
        if (entity.getState() == EntityState.NEW) {
            // 新建状态的实体，改为插入
            submit(LandTask.ofInsert(entity), false);
        } else {
            submit(LandTask.ofUpdate(entity), false);
        }
    }

    /**
     * 提交删除任务
     */
    public void submitDelete(BaseEntity<?> entity) {
        EntityState prevState = entity.getState();
        entity.setState(EntityState.DELETED);
        
        // 如果是 NEW 状态（还未落地）直接删除，则不需要入队
        // 因为数据库里还没有这条记录
        if (prevState == EntityState.NEW && entity.isInLandQueue()) {
            // 标记为不需要落地，让队列中的 INSERT 任务变成过期任务
            entity.syncVersion();
            logger.debug("Entity deleted before insert, skipping: {}", entity);
            return;
        }
        
        // 删除操作强制入队，即使已在队列中
        submit(LandTask.ofDelete(entity), true);
    }

    private void submit(LandTask task, boolean force) {
        if (shutdown.get()) {
            logger.warn("AsyncLandManager is shutdown, task rejected");
            return;
        }

        BaseEntity<?> entity = task.getEntity();
        
        // 已在队列中的不重复添加（除非强制）
        if (!force && entity.isInLandQueue()) {
            return;
        }
        entity.setInLandQueue(true);
        
        // 添加到脏数据缓存
        addToDirtyCache(entity);

        // 根据分片策略选择工作线程
        int workerIndex = selectWorker(entity);
        workerThreads[workerIndex].submit(task);

        totalTasks.incrementAndGet();
    }
    
    /**
     * 选择工作线程
     * <p>
     * 优先级：@WorkerIndex 注解 > 按表名 hash
     */
    private int selectWorker(BaseEntity<?> entity) {
        Class<?> entityClass = entity.getClass();
        
        // 1. 优先使用 @WorkerIndex 注解指定的线程（带缓存）
        int annotatedIndex = getAnnotatedWorkerIndex(entityClass);
        if (annotatedIndex >= 0 && annotatedIndex < workerThreads.length) {
            return annotatedIndex;
        }
        
        // 2. 按表名 hash：同一表固定到同一线程
        // 注意：Math.abs(Integer.MIN_VALUE) 仍为负数，使用位运算确保非负
        int hash = entityClass.hashCode() & 0x7FFFFFFF;
        return hash % workerThreads.length;
    }
    
    /**
     * 获取类上 @WorkerIndex 注解的值（带缓存）
     */
    private int getAnnotatedWorkerIndex(Class<?> entityClass) {
        return annotationCache.computeIfAbsent(entityClass, clazz -> {
            WorkerIndex annotation = clazz.getAnnotation(WorkerIndex.class);
            return annotation != null ? annotation.value() : -1;
        });
    }
    
    /**
     * 获取工作线程数量
     */
    public int getWorkerCount() {
        return workerThreads.length;
    }
    
    /**
     * 动态调整批量大小（所有工作线程）
     * <p>
     * 使用场景：
     * <ul>
     *   <li>大战期间：增大批量提高吞吐量</li>
     *   <li>低峰期：减小批量降低延迟</li>
     * </ul>
     */
    public void setBatchSize(int batchSize) {
        for (WorkerThread worker : workerThreads) {
            worker.setBatchSize(batchSize);
        }
        logger.info("Batch size updated to {}", batchSize);
    }
    
    /**
     * 动态调整落地间隔（所有工作线程）
     * <p>
     * 使用场景：
     * <ul>
     *   <li>大战期间：缩短间隔（500-1000ms）更快落地</li>
     *   <li>资源产出：延长间隔（2000-3000ms）充分合并</li>
     * </ul>
     */
    public void setLandIntervalMs(long landIntervalMs) {
        for (WorkerThread worker : workerThreads) {
            worker.setLandIntervalMs(landIntervalMs);
        }
        logger.info("Land interval updated to {} ms", landIntervalMs);
    }
    
    /**
     * 启用/禁用自适应调整（所有工作线程）
     * <p>
     * 自适应调整会根据每个线程的队列长度自动调整 batchSize 和 landIntervalMs
     */
    public void setAdaptiveEnabled(boolean enabled) {
        for (WorkerThread worker : workerThreads) {
            worker.setAdaptiveEnabled(enabled);
        }
        logger.info("Adaptive adjustment {}", enabled ? "enabled" : "disabled");
    }
    
    /**
     * 设置队列积压阈值（所有工作线程）
     * <p>
     * 当队列长度超过此值时，自动缩短间隔、增大批量
     */
    public void setBacklogThreshold(int threshold) {
        for (WorkerThread worker : workerThreads) {
            worker.setBacklogThreshold(threshold);
        }
        logger.info("Backlog threshold set to {}", threshold);
    }
    
    /**
     * 设置队列空闲阈值（所有工作线程）
     * <p>
     * 当队列长度低于此值时，自动延长间隔、减小批量
     */
    public void setIdleThreshold(int threshold) {
        for (WorkerThread worker : workerThreads) {
            worker.setIdleThreshold(threshold);
        }
        logger.info("Idle threshold set to {}", threshold);
    }
    
    /**
     * 添加到脏数据缓存
     */
    private void addToDirtyCache(BaseEntity<?> entity) {
        Object cacheKey = createCacheKey(entity);
        dirtyCache.computeIfAbsent(entity.getClass(), k -> new ConcurrentHashMap<>())
                .put(cacheKey, entity);
    }
    
    /**
     * 从脏数据缓存移除
     * <p>
     * 注意：使用条件移除，只有当缓存中的实体与传入的是同一对象时才移除。
     * 这样可以避免移除其他线程刚添加的新版本实体。
     */
    private void removeFromDirtyCache(BaseEntity<?> entity) {
        Object cacheKey = createCacheKey(entity);
        ConcurrentHashMap<Object, BaseEntity<?>> classCache = dirtyCache.get(entity.getClass());
        if (classCache != null) {
            // 使用条件移除：只有当缓存中的对象就是当前对象时才移除
            // 避免移除其他线程刚添加的新版本实体
            classCache.remove(cacheKey, entity);
        }
    }
    
    /**
     * 创建缓存 key（支持复合主键）
     * <p>
     * 注意：使用 Arrays.asList 而非 List.of，因为主键值可能包含 null
     */
    private Object createCacheKey(BaseEntity<?> entity) {
        Object[] pkValues = entity.getPrimaryKeyValues();
        if (pkValues.length == 1) {
            return pkValues[0];
        }
        // 使用 Arrays.asList 支持 null 元素（List.of 不支持 null）
        return java.util.Arrays.asList(pkValues);
    }

    // ==================== 处理落地 ====================

    /**
     * 批量处理任务（由 WorkerThread 调用）
     * <p>
     * 优化：单次遍历完成过滤+类型分组+表分组，减少遍历次数
     */
    private void processBatch(List<LandTask> tasks) {
        // 使用三维结构：TaskType -> EntityClass -> List<LandTask>
        // 单次遍历完成所有分组
        Map<TaskType, Map<Class<?>, List<LandTask>>> grouped = new EnumMap<>(TaskType.class);
        
        for (LandTask task : tasks) {
            BaseEntity<?> entity = task.getEntity();
            entity.setInLandQueue(false);

            // 根据任务类型决定是否跳过
            TaskType taskType = task.getType();
            EntityState entityState = entity.getState();
            
            switch (taskType) {
                case INSERT:
                    // INSERT 任务但实体已被删除 -> 跳过（创建后立即删除的情况）
                    if (entityState == EntityState.DELETED) {
                        logger.debug("Skipping INSERT for deleted entity: {}", entity);
                        removeFromDirtyCache(entity);  // 从脏数据缓存移除
                        continue;
                    }
                    break;
                    
                case UPDATE:
                    // UPDATE 任务但实体已被删除 -> 跳过
                    if (entityState == EntityState.DELETED) {
                        logger.debug("Skipping UPDATE for deleted entity: {}", entity);
                        // 注意：不需要 removeFromDirtyCache，因为 DELETE 任务会处理
                        // 且 DELETE 任务会覆盖脏数据缓存中的同一 key
                        continue;
                    }
                    break;
                    
                case DELETE:
                    // DELETE 任务总是执行
                    break;
            }

            // 按类型和表分组（单次遍历完成）
            grouped.computeIfAbsent(taskType, k -> new LinkedHashMap<>())
                    .computeIfAbsent(entity.getClass(), k -> new ArrayList<>())
                    .add(task);
        }

        // 按顺序处理：先删除，再插入，最后更新
        processTaskTypeGroup(grouped.get(TaskType.DELETE));
        processTaskTypeGroup(grouped.get(TaskType.INSERT));
        processTaskTypeGroup(grouped.get(TaskType.UPDATE));
    }

    /**
     * 处理某一类型的所有任务（已按表分组）
     */
    private void processTaskTypeGroup(Map<Class<?>, List<LandTask>> byTable) {
        if (byTable == null || byTable.isEmpty()) {
            return;
        }
        for (List<LandTask> tableTasks : byTable.values()) {
            processTableTasks(tableTasks);
        }
    }

    /**
     * 处理单张表的任务批次
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void processTableTasks(List<LandTask> tasks) {
        TaskType type = tasks.get(0).getType();
        // 使用传统 for 循环代替 Stream，减少高频调用时的对象创建开销
        List<BaseEntity<?>> entities = new ArrayList<>(tasks.size());
        for (LandTask task : tasks) {
            entities.add(task.getEntity());
        }

        try {
            int[] results;
            switch (type) {
                case INSERT -> results = sqlExecutor.batchInsert((List) entities);
                case UPDATE -> results = sqlExecutor.batchUpdate((List) entities);
                case DELETE -> results = sqlExecutor.batchDelete((List) entities);
                default -> results = new int[0];
            }

            // 处理结果
            // 注意：results.length 可能与 tasks.size() 不同（某些 JDBC 驱动行为）
            int resultCount = Math.min(results.length, tasks.size());
            for (int i = 0; i < resultCount; i++) {
                LandTask t = tasks.get(i);
                // Statement.SUCCESS_NO_INFO (-2) 也视为成功
                if (results[i] == 0) {
                    handleFailedTask(t);
                } else {
                    // 落地成功，从脏数据缓存移除
                    removeFromDirtyCache(t.getEntity());
                    // 同步版本
                    t.getEntity().syncVersion();
                    successTasks.incrementAndGet();
                }
            }
            // 如果 results 比 tasks 短，剩余任务视为失败
            for (int i = resultCount; i < tasks.size(); i++) {
                handleFailedTask(tasks.get(i));
            }
        } catch (Exception e) {
            logger.error("Batch {} failed for table {}", type, tasks.get(0).getEntity().getClass().getSimpleName(), e);
            // 全部失败，放回队列重试
            for (LandTask t : tasks) {
                handleFailedTask(t);
            }
        }
    }

    private void handleFailedTask(LandTask task) {
        task.incrementRetryCount();
        retryCount.incrementAndGet();  // 记录重试次数（监控用）

        if (task.getRetryCount() < config.getMaxRetries()) {
            // 重新提交到对应的工作线程
            BaseEntity<?> entity = task.getEntity();
            int workerIndex = selectWorker(entity);
            workerThreads[workerIndex].submit(task);
            entity.setInLandQueue(true);
            
            logger.warn("Task retry {}/{}: {}", task.getRetryCount(), config.getMaxRetries(), entity);
        } else {
            // 达到最大重试次数，计入最终失败
            failedTasks.incrementAndGet();
            // 从脏数据缓存移除，防止内存泄漏
            removeFromDirtyCache(task.getEntity());
            logger.error("Task exceeded max retries, dropped: {}", task.getEntity());
        }
    }

    // ==================== 同步落地 ====================

    /**
     * 立即同步落地指定实体
     */
    public boolean landNow(BaseEntity<?> entity) {
        return switch (entity.getState()) {
            case NEW -> doInsert(entity);
            case PERSISTENT -> !entity.hasChanges() || doUpdatePartial(entity);
            case DELETED -> doDelete(entity);
            default -> false;
        };
    }

    @SuppressWarnings("unchecked")
    private <T extends BaseEntity<T>> boolean doInsert(BaseEntity<?> entity) {
        return sqlExecutor.insert((T) entity);
    }

    @SuppressWarnings("unchecked")
    private <T extends BaseEntity<T>> boolean doUpdatePartial(BaseEntity<?> entity) {
        return sqlExecutor.updatePartial((T) entity);
    }

    @SuppressWarnings("unchecked")
    private <T extends BaseEntity<T>> boolean doDelete(BaseEntity<?> entity) {
        return sqlExecutor.delete((T) entity);
    }
    // ==================== 生命周期 ====================

    /**
     * 关闭（等待所有任务完成）
     * <p>
     * 此方法会阻塞直到所有待落地数据都写入数据库，确保数据不丢失
     */
    public void shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            logger.info("Shutting down AsyncLandManager, waiting for all tasks to complete...");
            
            // 发送毒丸，通知所有工作线程停止
            for (WorkerThread worker : workerThreads) {
                worker.shutdownWorker();
            }
            
            // 无限等待所有工作线程完成（确保数据落地）
            for (WorkerThread worker : workerThreads) {
                while (worker.isAlive()) {
                    try {
                        worker.join();  // 无超时，一直等待
                    } catch (InterruptedException e) {
                        // 即使被中断也要继续等待，确保数据不丢失
                        logger.warn("Shutdown interrupted, but continuing to wait for data persistence");
                    }
                }
            }
            
            logger.info("AsyncLandManager shutdown completed. Total: {}, Success: {}, Failed: {}",
                    totalTasks.get(), successTasks.get(), failedTasks.get());
        }
    }

    // ==================== 脏数据查询 ====================

    /**
     * 从脏数据缓存中查询实体
     * <p>
     * 如果实体在队列中尚未落地，返回该实体；否则返回 null
     * 
     * @param entityClass 实体类型
     * @param primaryKey 主键值（单主键直接传值，复合主键传数组）
     * @return 未落地的实体，或 null
     */
    @SuppressWarnings("unchecked")
    public <T extends BaseEntity<T>> T getDirty(Class<T> entityClass, Object primaryKey) {
        ConcurrentHashMap<Object, BaseEntity<?>> classCache = dirtyCache.get(entityClass);
        if (classCache == null) {
            return null;
        }
        BaseEntity<?> entity = classCache.get(primaryKey);
        // 如果实体已被删除，视为不存在
        if (entity != null && entity.getState() == EntityState.DELETED) {
            return null;
        }
        return (T) entity;
    }
    
    /**
     * 从脏数据缓存中查询实体（复合主键）
     * 
     * @param entityClass 实体类型
     * @param primaryKeys 主键值数组
     * @return 未落地的实体，或 null
     */
    public <T extends BaseEntity<T>> T getDirtyComposite(Class<T> entityClass, Object... primaryKeys) {
        return getDirty(entityClass, java.util.Arrays.asList(primaryKeys));
    }
    
    /**
     * 检查实体是否在脏数据缓存中（包括已删除的）
     * 
     * @param entityClass 实体类型
     * @param primaryKey 主键值
     * @return 是否在缓存中
     */
    public boolean isInDirtyCache(Class<?> entityClass, Object primaryKey) {
        ConcurrentHashMap<Object, BaseEntity<?>> classCache = dirtyCache.get(entityClass);
        return classCache != null && classCache.containsKey(primaryKey);
    }
    
    /**
     * 检查实体是否已被标记删除（在脏数据中）
     * 
     * @param entityClass 实体类型
     * @param primaryKey 主键值
     * @return 是否已删除
     */
    public boolean isDeleted(Class<?> entityClass, Object primaryKey) {
        ConcurrentHashMap<Object, BaseEntity<?>> classCache = dirtyCache.get(entityClass);
        if (classCache == null) {
            return false;
        }
        BaseEntity<?> entity = classCache.get(primaryKey);
        return entity != null && entity.getState() == EntityState.DELETED;
    }
    
    /**
     * 获取某类型的所有脏数据
     * 
     * @param entityClass 实体类型
     * @return 所有未落地的实体（不包括已删除的）
     */
    @SuppressWarnings("unchecked")
    public <T extends BaseEntity<T>> List<T> getAllDirty(Class<T> entityClass) {
        ConcurrentHashMap<Object, BaseEntity<?>> classCache = dirtyCache.get(entityClass);
        if (classCache == null) {
            return Collections.emptyList();
        }
        // 使用传统 for 循环代替 Stream，减少高频调用时的对象创建开销
        List<T> result = new ArrayList<>();
        for (BaseEntity<?> entity : classCache.values()) {
            if (entity.getState() != EntityState.DELETED) {
                result.add((T) entity);
            }
        }
        return result;
    }
    
    /**
     * 获取脏数据缓存大小
     */
    public int getDirtyCacheSize() {
        int size = 0;
        for (ConcurrentHashMap<Object, BaseEntity<?>> cache : dirtyCache.values()) {
            size += cache.size();
        }
        return size;
    }

    // ==================== 统计信息 ====================

    public long getTotalTasks() {
        return totalTasks.get();
    }

    public long getSuccessTasks() {
        return successTasks.get();
    }

    public long getFailedTasks() {
        return failedTasks.get();
    }
    
    public long getRetryCount() {
        return retryCount.get();
    }

    public int getPendingTasks() {
        int count = 0;
        for (WorkerThread worker : workerThreads) {
            count += worker.getQueueSize();
        }
        return count;
    }

}
