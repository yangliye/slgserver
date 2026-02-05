package com.muyi.db.async;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.muyi.db.annotation.Column;
import com.muyi.db.annotation.PrimaryKey;
import com.muyi.db.annotation.Table;
import com.muyi.db.core.BaseEntity;
import com.muyi.db.core.EntityState;
import com.muyi.db.sql.SqlExecutor;

/**
 * AsyncLandManager 压力测试
 */
class AsyncLandManagerBenchmark {

    // ==================== 压测配置 ====================
    
    /** 总实体数量 */
    private static final int TOTAL_ENTITIES = 100_000;
    
    /** 并发线程数 */
    private static final int CONCURRENT_THREADS = 10;
    
    /** 工作线程数 */
    private static final int WORKER_THREADS = 4;
    
    /** 批量大小 */
    private static final int BATCH_SIZE = 100;
    
    /** 落地间隔 (ms) */
    private static final long LAND_INTERVAL_MS = 50;

    private BenchmarkSqlExecutor mockExecutor;
    private AsyncLandManager landManager;

    @BeforeEach
    void setUp() {
        mockExecutor = new BenchmarkSqlExecutor();
        AsyncLandConfig config = new AsyncLandConfig()
                .landThreads(WORKER_THREADS)
                .landIntervalMs(LAND_INTERVAL_MS)
                .batchSize(BATCH_SIZE)
                .maxRetries(3);
        landManager = new AsyncLandManager(mockExecutor, config);
    }

    @AfterEach
    void tearDown() {
        if (landManager != null) {
            landManager.shutdown();
        }
    }

    // ==================== 压测用例 ====================

    @Test
    @DisplayName("压测1: 单线程顺序提交 INSERT")
    void benchmark_SingleThread_Insert() throws InterruptedException {
        System.out.println("\n========== 单线程顺序提交 INSERT ==========");
        System.out.printf("配置: 实体数=%d, 工作线程=%d, 批量大小=%d%n", 
                TOTAL_ENTITIES, WORKER_THREADS, BATCH_SIZE);
        
        long startTime = System.currentTimeMillis();
        
        // 提交阶段
        long submitStart = System.currentTimeMillis();
        for (int i = 0; i < TOTAL_ENTITIES; i++) {
            BenchmarkEntity entity = new BenchmarkEntity(i);
            entity.setData("data-" + i);
            landManager.submitInsert(entity);
        }
        long submitEnd = System.currentTimeMillis();
        
        System.out.printf("提交耗时: %d ms, 提交速率: %.2f ops/s%n", 
                submitEnd - submitStart, 
                TOTAL_ENTITIES * 1000.0 / (submitEnd - submitStart));
        
        // 等待落地完成
        waitForCompletion();
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // 输出结果
        printResult(totalTime);
    }

    @Test
    @DisplayName("压测2: 多线程并发提交 INSERT")
    void benchmark_MultiThread_Insert() throws InterruptedException {
        System.out.println("\n========== 多线程并发提交 INSERT ==========");
        System.out.printf("配置: 实体数=%d, 并发线程=%d, 工作线程=%d, 批量大小=%d%n", 
                TOTAL_ENTITIES, CONCURRENT_THREADS, WORKER_THREADS, BATCH_SIZE);
        
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(CONCURRENT_THREADS);
        AtomicInteger submittedCount = new AtomicInteger(0);
        
        int entitiesPerThread = TOTAL_ENTITIES / CONCURRENT_THREADS;
        
        long startTime = System.currentTimeMillis();
        
        // 启动并发线程
        for (int t = 0; t < CONCURRENT_THREADS; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await(); // 等待同时开始
                    
                    int startId = threadId * entitiesPerThread;
                    for (int i = 0; i < entitiesPerThread; i++) {
                        BenchmarkEntity entity = new BenchmarkEntity(startId + i);
                        entity.setData("data-" + (startId + i));
                        landManager.submitInsert(entity);
                        submittedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        // 开始计时并发提交
        long submitStart = System.currentTimeMillis();
        startLatch.countDown();
        endLatch.await();
        long submitEnd = System.currentTimeMillis();
        
        System.out.printf("提交耗时: %d ms, 提交速率: %.2f ops/s%n", 
                submitEnd - submitStart, 
                submittedCount.get() * 1000.0 / (submitEnd - submitStart));
        
        // 等待落地完成
        waitForCompletion();
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        executor.shutdown();
        
        // 输出结果
        printResult(totalTime);
    }

    @Test
    @DisplayName("压测3: 混合操作 (INSERT + UPDATE + DELETE)")
    void benchmark_MixedOperations() throws InterruptedException {
        System.out.println("\n========== 混合操作压测 ==========");
        int insertCount = TOTAL_ENTITIES / 2;
        int updateCount = TOTAL_ENTITIES / 4;
        int deleteCount = TOTAL_ENTITIES / 4;
        
        System.out.printf("配置: INSERT=%d, UPDATE=%d, DELETE=%d%n", 
                insertCount, updateCount, deleteCount);
        
        // 预先创建一些已持久化的实体用于 UPDATE 和 DELETE
        List<BenchmarkEntity> persistentEntities = new ArrayList<>();
        for (int i = 0; i < updateCount + deleteCount; i++) {
            BenchmarkEntity entity = new BenchmarkEntity(TOTAL_ENTITIES + i);
            entity.setData("persistent-" + i);
            entity.setState(EntityState.PERSISTENT);
            entity.syncVersion();
            entity.clearChanges();
            persistentEntities.add(entity);
        }
        
        long startTime = System.currentTimeMillis();
        
        // 提交 INSERT
        for (int i = 0; i < insertCount; i++) {
            BenchmarkEntity entity = new BenchmarkEntity(i);
            entity.setData("insert-" + i);
            landManager.submitInsert(entity);
        }
        
        // 提交 UPDATE
        for (int i = 0; i < updateCount; i++) {
            BenchmarkEntity entity = persistentEntities.get(i);
            entity.setData("updated-" + i);
            landManager.submitUpdate(entity);
        }
        
        // 提交 DELETE
        for (int i = updateCount; i < updateCount + deleteCount; i++) {
            landManager.submitDelete(persistentEntities.get(i));
        }
        
        // 等待落地完成
        waitForCompletion();
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // 输出结果
        System.out.println("\n---------- 压测结果 ----------");
        System.out.printf("总耗时: %d ms%n", totalTime);
        System.out.printf("INSERT 执行次数: %d%n", mockExecutor.insertCount.get());
        System.out.printf("UPDATE 执行次数: %d%n", mockExecutor.updateCount.get());
        System.out.printf("DELETE 执行次数: %d%n", mockExecutor.deleteCount.get());
        System.out.printf("总吞吐量: %.2f ops/s%n", 
                (mockExecutor.insertCount.get() + mockExecutor.updateCount.get() + mockExecutor.deleteCount.get()) 
                * 1000.0 / totalTime);
    }

    @Test
    @DisplayName("压测4: 高频更新同一实体")
    void benchmark_FrequentUpdate_SameEntity() throws InterruptedException {
        System.out.println("\n========== 高频更新同一实体 ==========");
        int updateTimes = 10_000;
        
        BenchmarkEntity entity = new BenchmarkEntity(1L);
        entity.setData("initial");
        entity.setState(EntityState.PERSISTENT);
        entity.syncVersion();
        entity.clearChanges();
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < updateTimes; i++) {
            entity.setData("update-" + i);
            landManager.submitUpdate(entity);
        }
        
        waitForCompletion();
        
        long endTime = System.currentTimeMillis();
        
        System.out.println("\n---------- 压测结果 ----------");
        System.out.printf("提交次数: %d%n", updateTimes);
        System.out.printf("实际 UPDATE 执行次数: %d%n", mockExecutor.updateCount.get());
        System.out.printf("合并率: %.2f%%%n", (1 - (double) mockExecutor.updateCount.get() / updateTimes) * 100);
        System.out.printf("总耗时: %d ms%n", endTime - startTime);
    }

    @Test
    @DisplayName("压测5: 不同工作线程数对比")
    void benchmark_DifferentWorkerThreads() throws InterruptedException {
        System.out.println("\n========== 不同工作线程数对比 ==========");
        
        int[] workerCounts = {1, 2, 4, 8};
        int testEntities = 50_000;
        
        for (int workers : workerCounts) {
            // 重新创建 landManager
            if (landManager != null) {
                landManager.shutdown();
            }
            mockExecutor = new BenchmarkSqlExecutor();
            AsyncLandConfig config = new AsyncLandConfig()
                    .landThreads(workers)
                    .landIntervalMs(LAND_INTERVAL_MS)
                    .batchSize(BATCH_SIZE)
                    .maxRetries(3);
            landManager = new AsyncLandManager(mockExecutor, config);
            
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < testEntities; i++) {
                BenchmarkEntity entity = new BenchmarkEntity(i);
                entity.setData("data-" + i);
                landManager.submitInsert(entity);
            }
            
            waitForCompletion();
            
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            
            System.out.printf("工作线程=%d: 耗时=%d ms, 吞吐量=%.2f ops/s%n", 
                    workers, totalTime, testEntities * 1000.0 / totalTime);
        }
    }

    // ==================== 辅助方法 ====================

    private void waitForCompletion() throws InterruptedException {
        // 等待队列清空
        int maxWaitSeconds = 60;
        int waitedMs = 0;
        int checkIntervalMs = 100;
        
        while (waitedMs < maxWaitSeconds * 1000) {
            int pending = landManager.getPendingTasks();
            if (pending == 0) {
                // 再等一下确保处理完成
                Thread.sleep(200);
                if (landManager.getPendingTasks() == 0) {
                    break;
                }
            }
            Thread.sleep(checkIntervalMs);
            waitedMs += checkIntervalMs;
        }
    }

    private void printResult(long totalTime) {
        System.out.println("\n---------- 压测结果 ----------");
        System.out.printf("总耗时: %d ms%n", totalTime);
        System.out.printf("INSERT 执行次数: %d%n", mockExecutor.insertCount.get());
        System.out.printf("总任务数: %d%n", landManager.getTotalTasks());
        System.out.printf("成功任务数: %d%n", landManager.getSuccessTasks());
        System.out.printf("失败任务数: %d%n", landManager.getFailedTasks());
        System.out.printf("吞吐量: %.2f ops/s%n", mockExecutor.insertCount.get() * 1000.0 / totalTime);
        System.out.printf("平均延迟: %.3f ms/op%n", (double) totalTime / mockExecutor.insertCount.get());
    }

    // ==================== 测试实体 ====================

    @Table("benchmark_entity")
    public static class BenchmarkEntity extends BaseEntity<BenchmarkEntity> {
        @PrimaryKey
        @Column("id")
        private long id;

        @Column("data")
        private String data;

        public BenchmarkEntity() {}

        public BenchmarkEntity(long id) {
            this.id = id;
        }

        public long getId() { return id; }
        public void setId(long id) { this.id = id; markChanged("id"); }
        public String getData() { return data; }
        public void setData(String data) { this.data = data; markChanged("data"); }
    }

    // ==================== Mock SqlExecutor ====================

    private static class BenchmarkSqlExecutor extends SqlExecutor {
        final AtomicLong insertCount = new AtomicLong(0);
        final AtomicLong updateCount = new AtomicLong(0);
        final AtomicLong deleteCount = new AtomicLong(0);

        public BenchmarkSqlExecutor() {
            super(null);
        }

        @Override
        public <T extends BaseEntity<T>> boolean insert(T entity) {
            insertCount.incrementAndGet();
            entity.setState(EntityState.PERSISTENT);
            entity.clearChanges();
            entity.syncVersion();
            return true;
        }

        @Override
        public <T extends BaseEntity<T>> boolean update(T entity) {
            updateCount.incrementAndGet();
            entity.clearChanges();
            entity.syncVersion();
            return true;
        }

        @Override
        public <T extends BaseEntity<T>> boolean updatePartial(T entity) {
            return update(entity);
        }

        @Override
        public <T extends BaseEntity<T>> boolean delete(T entity) {
            deleteCount.incrementAndGet();
            return true;
        }

        @Override
        public <T extends BaseEntity<T>> int[] batchInsert(List<T> entities) {
            int[] results = new int[entities.size()];
            for (int i = 0; i < entities.size(); i++) {
                insert(entities.get(i));
                results[i] = 1;
            }
            return results;
        }

        @Override
        public <T extends BaseEntity<T>> int[] batchUpdate(List<T> entities) {
            int[] results = new int[entities.size()];
            for (int i = 0; i < entities.size(); i++) {
                update(entities.get(i));
                results[i] = 1;
            }
            return results;
        }

        @Override
        public <T extends BaseEntity<T>> int[] batchDelete(List<T> entities) {
            int[] results = new int[entities.size()];
            for (int i = 0; i < entities.size(); i++) {
                delete(entities.get(i));
                results[i] = 1;
            }
            return results;
        }
    }

    // ==================== 自适应调整压测 ====================

    @Test
    @DisplayName("压测6: 自适应调整 - 高负载场景")
    void benchmark_Adaptive_HighLoad() throws InterruptedException {
        System.out.println("\n========== 自适应调整 - 高负载场景 ==========");
        System.out.println("测试：队列积压时是否自动缩短间隔、增大批量");
        
        // 确保启用自适应
        landManager.setAdaptiveEnabled(true);
        landManager.setBacklogThreshold(500);  // 降低阈值便于触发
        
        int totalEntities = 20_000;
        long startTime = System.currentTimeMillis();
        
        // 快速提交大量任务，造成积压
        for (int i = 0; i < totalEntities; i++) {
            BenchmarkEntity entity = new BenchmarkEntity(i);
            entity.setData("data-" + i);
            landManager.submitInsert(entity);
        }
        
        long submitTime = System.currentTimeMillis() - startTime;
        System.out.printf("提交耗时: %d ms, 提交速率: %.2f ops/s%n", 
                submitTime, totalEntities * 1000.0 / submitTime);
        
        // 等待处理完成
        waitForCompletion();
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        System.out.println("\n---------- 压测结果 ----------");
        System.out.printf("总耗时: %d ms%n", totalTime);
        System.out.printf("INSERT 执行次数: %d%n", mockExecutor.insertCount.get());
        System.out.printf("吞吐量: %.2f ops/s%n", mockExecutor.insertCount.get() * 1000.0 / totalTime);
        System.out.println("注意：查看日志中的 'adaptive' 关键字，确认是否触发了自适应调整");
    }

    @Test
    @DisplayName("压测7: 自适应调整 - 低负载场景")
    void benchmark_Adaptive_LowLoad() throws InterruptedException {
        System.out.println("\n========== 自适应调整 - 低负载场景 ==========");
        System.out.println("测试：队列空闲时是否自动延长间隔、减小批量");
        
        // 确保启用自适应
        landManager.setAdaptiveEnabled(true);
        landManager.setIdleThreshold(50);  // 降低阈值便于触发
        
        int totalEntities = 1000;
        long startTime = System.currentTimeMillis();
        
        // 缓慢提交少量任务
        for (int i = 0; i < totalEntities; i++) {
            BenchmarkEntity entity = new BenchmarkEntity(i);
            entity.setData("data-" + i);
            landManager.submitInsert(entity);
            Thread.sleep(5);  // 慢速提交，让队列保持空闲
        }
        
        long submitTime = System.currentTimeMillis() - startTime;
        System.out.printf("提交耗时: %d ms%n", submitTime);
        
        // 等待处理完成
        waitForCompletion();
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        System.out.println("\n---------- 压测结果 ----------");
        System.out.printf("总耗时: %d ms%n", totalTime);
        System.out.printf("INSERT 执行次数: %d%n", mockExecutor.insertCount.get());
        System.out.println("注意：查看日志中的 'adaptive' 关键字，确认是否触发了自适应调整");
    }

    @Test
    @DisplayName("压测8: 自适应调整 vs 固定配置对比")
    void benchmark_Adaptive_VS_Fixed() throws InterruptedException {
        System.out.println("\n========== 自适应调整 vs 固定配置对比 ==========");
        
        int testEntities = 50_000;
        
        // 测试1：固定配置
        System.out.println("\n--- 测试1：固定配置（自适应关闭）---");
        if (landManager != null) {
            landManager.shutdown();
        }
        mockExecutor = new BenchmarkSqlExecutor();
        AsyncLandConfig config1 = new AsyncLandConfig()
                .landThreads(2)
                .landIntervalMs(1000)
                .batchSize(200);
        landManager = new AsyncLandManager(mockExecutor, config1);
        landManager.setAdaptiveEnabled(false);  // 关闭自适应
        
        long start1 = System.currentTimeMillis();
        for (int i = 0; i < testEntities; i++) {
            BenchmarkEntity entity = new BenchmarkEntity(i);
            entity.setData("fixed-" + i);
            landManager.submitInsert(entity);
        }
        waitForCompletion();
        long time1 = System.currentTimeMillis() - start1;
        long count1 = mockExecutor.insertCount.get();
        
        System.out.printf("固定配置: 耗时=%d ms, 吞吐量=%.2f ops/s%n", 
                time1, count1 * 1000.0 / time1);
        
        // 测试2：自适应配置
        System.out.println("\n--- 测试2：自适应配置（自适应开启）---");
        landManager.shutdown();
        mockExecutor = new BenchmarkSqlExecutor();
        AsyncLandConfig config2 = new AsyncLandConfig()
                .landThreads(2)
                .landIntervalMs(1000)
                .batchSize(200);
        landManager = new AsyncLandManager(mockExecutor, config2);
        landManager.setAdaptiveEnabled(true);   // 开启自适应
        landManager.setBacklogThreshold(1000);
        landManager.setIdleThreshold(100);
        
        long start2 = System.currentTimeMillis();
        for (int i = 0; i < testEntities; i++) {
            BenchmarkEntity entity = new BenchmarkEntity(i);
            entity.setData("adaptive-" + i);
            landManager.submitInsert(entity);
        }
        waitForCompletion();
        long time2 = System.currentTimeMillis() - start2;
        long count2 = mockExecutor.insertCount.get();
        
        System.out.printf("自适应配置: 耗时=%d ms, 吞吐量=%.2f ops/s%n", 
                time2, count2 * 1000.0 / time2);
        
        System.out.println("\n---------- 对比结果 ----------");
        double improvement = (time1 - time2) * 100.0 / time1;
        System.out.printf("性能提升: %.2f%%%n", improvement);
        System.out.println("注意：自适应调整会根据负载自动优化，在高负载时通常表现更好");
    }

    @Test
    @DisplayName("压测9: 自适应调整 - 波动负载场景")
    void benchmark_Adaptive_FluctuatingLoad() throws InterruptedException {
        System.out.println("\n========== 自适应调整 - 波动负载场景 ==========");
        System.out.println("测试：负载波动时自适应调整的响应能力");
        
        landManager.setAdaptiveEnabled(true);
        landManager.setBacklogThreshold(2000);
        landManager.setIdleThreshold(200);
        
        // 阶段1：高负载
        System.out.println("\n--- 阶段1：高负载（快速提交）---");
        for (int i = 0; i < 10_000; i++) {
            BenchmarkEntity entity = new BenchmarkEntity(i);
            entity.setData("high-" + i);
            landManager.submitInsert(entity);
        }
        Thread.sleep(500);  // 等待处理
        
        // 阶段2：低负载
        System.out.println("--- 阶段2：低负载（慢速提交）---");
        for (int i = 10_000; i < 12_000; i++) {
            BenchmarkEntity entity = new BenchmarkEntity(i);
            entity.setData("low-" + i);
            landManager.submitInsert(entity);
            Thread.sleep(10);
        }
        Thread.sleep(1000);
        
        // 阶段3：再次高负载
        System.out.println("--- 阶段3：再次高负载---");
        for (int i = 12_000; i < 22_000; i++) {
            BenchmarkEntity entity = new BenchmarkEntity(i);
            entity.setData("high2-" + i);
            landManager.submitInsert(entity);
        }
        
        waitForCompletion();
        
        System.out.println("\n---------- 压测结果 ----------");
        System.out.printf("总 INSERT 执行次数: %d%n", mockExecutor.insertCount.get());
        System.out.println("注意：查看日志，应该能看到自适应调整在不同阶段的响应");
    }

    // ==================== 极限吞吐量压测 ====================

    @Test
    @DisplayName("极限压测: 最大吞吐量测试（默认配置）")
    void benchmark_MaxThroughput() throws InterruptedException {
        System.out.println("\n========== 极限吞吐量压测（默认配置） ==========");
        
        // 使用默认配置（不设置参数，使用默认值）
        if (landManager != null) {
            landManager.shutdown();
        }
        mockExecutor = new BenchmarkSqlExecutor();
        AsyncLandConfig config = new AsyncLandConfig();  // 使用默认配置
        landManager = new AsyncLandManager(mockExecutor, config);
        landManager.setAdaptiveEnabled(false);  // 关闭自适应，使用固定配置
        
        int totalEntities = 1_000_000;  // 100万实体
        int concurrentThreads = 20;     // 20个并发提交线程
        
        System.out.printf("配置: 实体数=%d, 并发线程=%d, 工作线程=%d, 批量大小=%d, 间隔=%dms%n",
                totalEntities, concurrentThreads, config.getLandThreads(), 
                config.getBatchSize(), config.getLandIntervalMs());
        
        ExecutorService executor = Executors.newFixedThreadPool(concurrentThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(concurrentThreads);
        AtomicInteger submittedCount = new AtomicInteger(0);
        
        int entitiesPerThread = totalEntities / concurrentThreads;
        
        long startTime = System.currentTimeMillis();
        
        // 启动并发提交线程
        for (int t = 0; t < concurrentThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    int startId = threadId * entitiesPerThread;
                    for (int i = 0; i < entitiesPerThread; i++) {
                        BenchmarkEntity entity = new BenchmarkEntity(startId + i);
                        entity.setData("data-" + (startId + i));
                        landManager.submitInsert(entity);
                        submittedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        // 开始计时并发提交
        long submitStart = System.currentTimeMillis();
        startLatch.countDown();
        endLatch.await();
        long submitEnd = System.currentTimeMillis();
        long submitTime = submitEnd - submitStart;
        
        System.out.printf("\n提交阶段: 耗时=%d ms, 提交速率=%.2f ops/s%n",
                submitTime, submittedCount.get() * 1000.0 / submitTime);
        
        // 等待落地完成
        waitForCompletion();
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        long processTime = endTime - submitEnd;
        
        // 输出结果
        System.out.println("\n---------- 极限吞吐量结果 ----------");
        System.out.printf("总耗时: %d ms (%.2f 秒)%n", totalTime, totalTime / 1000.0);
        System.out.printf("提交耗时: %d ms%n", submitTime);
        System.out.printf("处理耗时: %d ms (%.2f 秒)%n", processTime, processTime / 1000.0);
        System.out.printf("INSERT 执行次数: %d%n", mockExecutor.insertCount.get());
        System.out.printf("总任务数: %d%n", landManager.getTotalTasks());
        System.out.printf("成功任务数: %d%n", landManager.getSuccessTasks());
        System.out.printf("失败任务数: %d%n", landManager.getFailedTasks());
        System.out.printf("\n【极限吞吐量】: %.2f ops/s (%.2f 万/秒)%n",
                mockExecutor.insertCount.get() * 1000.0 / processTime,
                mockExecutor.insertCount.get() * 1000.0 / processTime / 10000);
        System.out.printf("【平均延迟】: %.3f ms/op%n",
                (double) processTime / mockExecutor.insertCount.get());
    }

    @Test
    @DisplayName("极限压测: 不同配置对比")
    void benchmark_MaxThroughput_ConfigComparison() throws InterruptedException {
        System.out.println("\n========== 不同配置下的极限吞吐量对比 ==========");
        
        int testEntities = 500_000;  // 50万实体
        int[][] configs = {
            {2, 100, 100},   // 线程数, 间隔ms, 批量大小
            {4, 50, 200},
            {4, 50, 500},
            {8, 50, 500},
            {4, 100, 1000},
        };
        
        for (int[] cfg : configs) {
            int threads = cfg[0];
            int interval = cfg[1];
            int batch = cfg[2];
            
            if (landManager != null) {
                landManager.shutdown();
            }
            mockExecutor = new BenchmarkSqlExecutor();
            AsyncLandConfig config = new AsyncLandConfig()
                    .landThreads(threads)
                    .landIntervalMs(interval)
                    .batchSize(batch);
            landManager = new AsyncLandManager(mockExecutor, config);
            landManager.setAdaptiveEnabled(false);
            
            long startTime = System.currentTimeMillis();
            
            // 快速提交
            for (int i = 0; i < testEntities; i++) {
                BenchmarkEntity entity = new BenchmarkEntity(i);
                entity.setData("data-" + i);
                landManager.submitInsert(entity);
            }
            
            long submitTime = System.currentTimeMillis() - startTime;
            
            // 等待处理完成
            waitForCompletion();
            
            long endTime = System.currentTimeMillis();
            long processTime = endTime - startTime;
            
            System.out.printf("配置: threads=%d, interval=%dms, batch=%d | " +
                    "耗时=%d ms | 吞吐量=%.2f ops/s (%.2f 万/秒)%n",
                    threads, interval, batch, processTime,
                    mockExecutor.insertCount.get() * 1000.0 / processTime,
                    mockExecutor.insertCount.get() * 1000.0 / processTime / 10000);
        }
    }
}
