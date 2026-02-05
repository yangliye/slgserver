package com.muyi.db.async;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.muyi.db.annotation.PrimaryKey;
import com.muyi.db.annotation.Table;
import com.muyi.db.core.BaseEntity;
import com.muyi.db.core.EntityState;
import com.muyi.db.sql.SqlExecutor;

/**
 * 自适应调整有效性验证测试
 * 
 * 验证目标：
 * 1. 自适应调整是否真的有效
 * 2. 不同场景下的最优配置
 * 3. 固定配置 vs 自适应配置的对比
 */
class AdaptiveEffectivenessTest {

    // ==================== 测试1：固定配置 vs 自适应配置 ====================
    
    @Test
    @DisplayName("验证1：固定配置 vs 自适应配置 吞吐量对比")
    void test_FixedVsAdaptive() throws Exception {
        System.out.println("\n========== 验证1：固定配置 vs 自适应配置 ==========");
        
        int totalEntities = 300_000;
        
        // 场景A：关闭自适应（固定配置）
        System.out.println("\n--- 场景A：关闭自适应（固定配置） ---");
        TestResult fixedResult = runBenchmark(totalEntities, false, 4, 50, 200, 1000, 100);
        
        // 场景B：开启自适应
        System.out.println("\n--- 场景B：开启自适应 ---");
        TestResult adaptiveResult = runBenchmark(totalEntities, true, 4, 50, 200, 1000, 100);
        
        // 对比结果
        System.out.println("\n========== 对比结果 ==========");
        System.out.printf("固定配置:   吞吐量=%,.2f ops/s, 耗时=%d ms%n", 
                fixedResult.throughput, fixedResult.totalTime);
        System.out.printf("自适应配置: 吞吐量=%,.2f ops/s, 耗时=%d ms%n", 
                adaptiveResult.throughput, adaptiveResult.totalTime);
        
        double diff = (adaptiveResult.throughput - fixedResult.throughput) / fixedResult.throughput * 100;
        System.out.printf("性能差异: %.2f%% (%s)%n", 
                Math.abs(diff),
                diff > 1 ? "自适应更优" : diff < -1 ? "固定更优" : "基本相当");
    }
    
    // ==================== 测试2：突发负载场景 ====================
    
    @Test
    @DisplayName("验证2：突发负载场景下的响应能力")
    void test_BurstLoad() throws Exception {
        System.out.println("\n========== 验证2：突发负载场景 ==========");
        System.out.println("模拟：稳定负载 → 突然 10 倍峰值 → 恢复稳定\n");
        
        // 场景A：关闭自适应
        System.out.println("--- 关闭自适应 ---");
        long fixedTime = runBurstLoadTest(false);
        
        // 场景B：开启自适应
        System.out.println("\n--- 开启自适应 ---");
        long adaptiveTime = runBurstLoadTest(true);
        
        System.out.println("\n========== 突发负载对比 ==========");
        System.out.printf("固定配置总耗时:   %d ms%n", fixedTime);
        System.out.printf("自适应配置总耗时: %d ms%n", adaptiveTime);
        double improvement = (fixedTime - adaptiveTime) * 100.0 / fixedTime;
        System.out.printf("改善幅度: %.2f%%%n", improvement);
    }
    
    private long runBurstLoadTest(boolean adaptive) throws Exception {
        MockExecutor mockExecutor = new MockExecutor();
        AsyncLandConfig config = new AsyncLandConfig()
                .landThreads(4)
                .landIntervalMs(50)
                .batchSize(200);
        AsyncLandManager manager = new AsyncLandManager(mockExecutor, config);
        manager.setAdaptiveEnabled(adaptive);
        if (adaptive) {
            manager.setBacklogThreshold(500);
            manager.setIdleThreshold(50);
        }
        
        long startTime = System.currentTimeMillis();
        int idCounter = 0;
        
        // 阶段1：稳定负载（2000 条，慢速提交）
        System.out.println("  阶段1：稳定负载（2000 条）");
        for (int i = 0; i < 2000; i++) {
            AdaptiveEntity entity = new AdaptiveEntity(++idCounter);
            manager.submitInsert(entity);
            Thread.sleep(1);
        }
        System.out.printf("    队列积压: %d%n", manager.getPendingTasks());
        
        // 阶段2：突发高峰（瞬间 30000 条）
        System.out.println("  阶段2：突发高峰（30000 条）");
        long burstStart = System.currentTimeMillis();
        for (int i = 0; i < 30000; i++) {
            AdaptiveEntity entity = new AdaptiveEntity(++idCounter);
            manager.submitInsert(entity);
        }
        System.out.printf("    突发提交耗时: %d ms%n", System.currentTimeMillis() - burstStart);
        System.out.printf("    队列峰值积压: %d%n", manager.getPendingTasks());
        
        // 阶段3：等待恢复
        System.out.println("  阶段3：等待处理完成");
        while (manager.getPendingTasks() > 0) {
            Thread.sleep(50);
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.printf("    完成！总耗时: %d ms, INSERT: %d%n", totalTime, mockExecutor.insertCount.get());
        
        manager.shutdown();
        return totalTime;
    }
    
    // ==================== 测试3：寻找最优配置 ====================
    
    @Test
    @DisplayName("验证3：寻找最优固定配置")
    void test_FindOptimalConfig() throws Exception {
        System.out.println("\n========== 验证3：寻找最优固定配置 ==========");
        
        int testEntities = 200_000;
        
        // 测试不同配置组合
        int[][] configs = {
            // {线程数, 间隔ms, 批量大小}
            {2, 100, 100},
            {2, 50, 200},
            {4, 100, 100},
            {4, 50, 200},     // 当前默认配置
            {4, 25, 400},
            {4, 10, 500},
            {8, 50, 200},
            {8, 25, 400},
            {8, 10, 500},
        };
        
        List<ConfigResult> results = new ArrayList<>();
        
        System.out.println("\n正在测试各配置...\n");
        
        for (int[] cfg : configs) {
            TestResult result = runBenchmark(testEntities, false, cfg[0], cfg[1], cfg[2], 0, 0);
            results.add(new ConfigResult(cfg[0], cfg[1], cfg[2], result.throughput, result.totalTime));
        }
        
        // 排序并输出
        results.sort((a, b) -> Double.compare(b.throughput, a.throughput));
        
        System.out.println("\n========== 配置性能排名 ==========");
        System.out.printf("%-8s %-10s %-8s %-18s %-10s%n", 
                "线程数", "间隔(ms)", "批量", "吞吐量(ops/s)", "耗时(ms)");
        System.out.println("----------------------------------------------------------");
        
        for (int i = 0; i < results.size(); i++) {
            ConfigResult r = results.get(i);
            String rank = (i == 0) ? " ★ 最优" : (i == 1) ? " ☆ 次优" : "";
            System.out.printf("%-8d %-10d %-8d %,-18.2f %-10d%s%n", 
                    r.threads, r.interval, r.batch, r.throughput, r.totalTime, rank);
        }
        
        ConfigResult best = results.get(0);
        System.out.printf("\n【推荐最优配置】%n");
        System.out.printf("  - 线程数: %d%n", best.threads);
        System.out.printf("  - 间隔: %d ms%n", best.interval);
        System.out.printf("  - 批量: %d%n", best.batch);
        System.out.printf("  - 最大吞吐量: %,.2f ops/s (%.2f 万/秒)%n", 
                best.throughput, best.throughput / 10000);
    }
    
    // ==================== 测试4：自适应参数效果观察 ====================
    
    @Test
    @DisplayName("验证4：观察自适应调整过程")
    void test_ObserveAdaptiveAdjustment() throws Exception {
        System.out.println("\n========== 验证4：观察自适应调整过程 ==========");
        System.out.println("实时观察队列积压和自适应状态变化\n");
        
        MockExecutor mockExecutor = new MockExecutor();
        AsyncLandConfig config = new AsyncLandConfig()
                .landThreads(4)
                .landIntervalMs(100)  // 故意设置较长间隔
                .batchSize(100);       // 故意设置较小批量
        AsyncLandManager manager = new AsyncLandManager(mockExecutor, config);
        manager.setAdaptiveEnabled(true);
        manager.setBacklogThreshold(300);  // 降低阈值以便观察
        manager.setIdleThreshold(30);
        
        int idCounter = 0;
        
        // 快速提交一批，触发 BACKLOG 状态
        System.out.println("快速提交 5000 条，观察自适应调整...");
        for (int i = 0; i < 5000; i++) {
            AdaptiveEntity entity = new AdaptiveEntity(++idCounter);
            manager.submitInsert(entity);
            
            // 每 500 条输出一次状态
            if (i > 0 && i % 500 == 0) {
                System.out.printf("  已提交: %d, 队列积压: %d%n", i, manager.getPendingTasks());
            }
        }
        
        System.out.println("\n等待处理（观察积压下降）...");
        while (manager.getPendingTasks() > 0) {
            System.out.printf("  队列积压: %d%n", manager.getPendingTasks());
            Thread.sleep(200);
        }
        
        System.out.printf("\n处理完成！INSERT 总数: %d%n", mockExecutor.insertCount.get());
        
        manager.shutdown();
    }
    
    // ==================== 辅助方法 ====================
    
    private TestResult runBenchmark(int totalEntities, boolean adaptive, 
            int threads, long interval, int batch, int backlogThreshold, int idleThreshold) throws Exception {
        
        MockExecutor mockExecutor = new MockExecutor();
        AsyncLandConfig config = new AsyncLandConfig()
                .landThreads(threads)
                .landIntervalMs(interval)
                .batchSize(batch);
        AsyncLandManager manager = new AsyncLandManager(mockExecutor, config);
        manager.setAdaptiveEnabled(adaptive);
        if (adaptive && backlogThreshold > 0) {
            manager.setBacklogThreshold(backlogThreshold);
            manager.setIdleThreshold(idleThreshold);
        }
        
        long startTime = System.currentTimeMillis();
        
        // 快速提交
        for (int i = 0; i < totalEntities; i++) {
            AdaptiveEntity entity = new AdaptiveEntity(i);
            manager.submitInsert(entity);
        }
        
        long submitTime = System.currentTimeMillis() - startTime;
        
        // 等待处理完成
        while (manager.getPendingTasks() > 0) {
            Thread.sleep(20);
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        long processedCount = mockExecutor.insertCount.get();
        double throughput = processedCount * 1000.0 / totalTime;
        
        System.out.printf("  threads=%d, interval=%dms, batch=%d, adaptive=%s%n", 
                threads, interval, batch, adaptive);
        System.out.printf("  耗时=%d ms, 吞吐量=%,.2f ops/s%n", totalTime, throughput);
        
        manager.shutdown();
        
        return new TestResult(totalTime, submitTime, processedCount, throughput);
    }
    
    // ==================== 辅助类 ====================
    
    static class TestResult {
        long totalTime;
        long submitTime;
        long processedCount;
        double throughput;
        
        TestResult(long totalTime, long submitTime, long processedCount, double throughput) {
            this.totalTime = totalTime;
            this.submitTime = submitTime;
            this.processedCount = processedCount;
            this.throughput = throughput;
        }
    }
    
    static class ConfigResult {
        int threads, interval, batch;
        double throughput;
        long totalTime;
        
        ConfigResult(int threads, int interval, int batch, double throughput, long totalTime) {
            this.threads = threads;
            this.interval = interval;
            this.batch = batch;
            this.throughput = throughput;
            this.totalTime = totalTime;
        }
    }
    
    @Table("adaptive_test")
    static class AdaptiveEntity extends BaseEntity<AdaptiveEntity> {
        @PrimaryKey
        private long id;
        private String data;
        
        public AdaptiveEntity() {}
        
        public AdaptiveEntity(long id) {
            this.id = id;
            this.data = "data-" + id;
        }
        
        public long getId() { return id; }
        public void setId(long id) { this.id = id; markChanged("id"); }
        public String getData() { return data; }
        public void setData(String data) { this.data = data; markChanged("data"); }
    }
    
    /**
     * Mock SQL 执行器
     */
    static class MockExecutor extends SqlExecutor {
        AtomicLong insertCount = new AtomicLong(0);
        AtomicLong updateCount = new AtomicLong(0);
        AtomicLong deleteCount = new AtomicLong(0);
        
        // 模拟数据库延迟（微秒）
        static final int LATENCY_US = 5;
        
        public MockExecutor() {
            super(null);
        }
        
        @Override
        public <T extends BaseEntity<T>> int[] batchInsert(List<T> entities) {
            simulateLatency(entities.size());
            
            int[] results = new int[entities.size()];
            for (int i = 0; i < entities.size(); i++) {
                results[i] = 1;
                entities.get(i).setState(EntityState.PERSISTENT);
            }
            insertCount.addAndGet(entities.size());
            return results;
        }
        
        @Override
        public <T extends BaseEntity<T>> int[] batchUpdate(List<T> entities) {
            simulateLatency(entities.size());
            int[] results = new int[entities.size()];
            for (int i = 0; i < entities.size(); i++) {
                results[i] = 1;
            }
            updateCount.addAndGet(entities.size());
            return results;
        }
        
        @Override
        public <T extends BaseEntity<T>> int[] batchDelete(List<T> entities) {
            simulateLatency(entities.size());
            int[] results = new int[entities.size()];
            for (int i = 0; i < entities.size(); i++) {
                results[i] = 1;
                entities.get(i).setState(EntityState.DELETED);
            }
            deleteCount.addAndGet(entities.size());
            return results;
        }
        
        private void simulateLatency(int batchSize) {
            // 模拟批量操作延迟：批量越大，单条平均延迟越低
            long delayUs = LATENCY_US * batchSize / 5 + 50;
            long delayNs = delayUs * 1000;
            long start = System.nanoTime();
            while (System.nanoTime() - start < delayNs) {
                // busy wait
            }
        }
    }
}
