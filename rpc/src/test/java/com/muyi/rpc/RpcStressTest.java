package com.muyi.rpc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.muyi.rpc.client.RpcProxyManager;
import com.muyi.rpc.game.GameServer;
import com.muyi.rpc.game.GameServerConfig;
import com.muyi.rpc.gamei.IGameService;
import com.muyi.rpc.gamei.PlayerInfo;

/**
 * RPC 压力测试
 * 测试 RPC 框架的性能上限，包括：
 * - QPS (每秒请求数)
 * - 延迟分布 (P50, P90, P99)
 * - 成功率
 * - 并发连接数
 * 
 * @author muyi
 */
public class RpcStressTest {
    
    private static final String ZK_ADDRESS = "127.0.0.1:2181";
    
    // 测试配置
    private static final int SERVER_PORT = 19010;
    private static final int SERVER_ID = 1;
    
    // 压力测试参数
    private static final int[] CONCURRENT_LEVELS = {10, 50, 100, 200, 500, 1000, 2000, 5000};
    private static final int WARMUP_SECONDS = 5;  // 预热时间
    private static final int TEST_DURATION_SECONDS = 30;  // 测试持续时间
    
    private static GameServer gameServer;
    private static RpcProxyManager rpcClient;
    
    public static void main(String[] args) throws Exception {
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║              RPC 压力测试 - Performance Benchmark               ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        // 启动服务器
        startServer();
        
        // 等待服务启动完成
        Thread.sleep(2000);
        
        // 初始化客户端
        initClient();
        
        // 预热
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("预热阶段 (" + WARMUP_SECONDS + " 秒)...");
        warmup();
        System.out.println("预热完成\n");
        
        // 执行压力测试
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("开始压力测试");
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println();
        
        for (int concurrency : CONCURRENT_LEVELS) {
            System.out.println("┌─────────────────────────────────────────────────────────────────┐");
            System.out.println("│  并发数: " + String.format("%5d", concurrency) + " 线程");
            System.out.println("├─────────────────────────────────────────────────────────────────┤");
            
            TestResult result = runStressTest(concurrency);
            printResult(result);
            
            System.out.println("└─────────────────────────────────────────────────────────────────┘");
            System.out.println();
            
            // 如果成功率太低，停止测试
            if (result.successRate < 0.5) {
                System.out.println("⚠️  成功率过低，停止测试");
                break;
            }
            
            // 短暂休息，让系统恢复
            Thread.sleep(2000);
        }
        
        // 清理
        cleanup();
        
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("压力测试完成");
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }
    
    /**
     * 启动 RPC 服务器
     */
    private static void startServer() throws Exception {
        System.out.println("[启动服务器]");
        GameServerConfig config = new GameServerConfig()
                .serverId(SERVER_ID)
                .port(SERVER_PORT)
                .zkAddress(ZK_ADDRESS);
        
        gameServer = new GameServer(config);
        gameServer.start();
        System.out.println("✓ 服务器已启动: port=" + SERVER_PORT + ", serverId=" + SERVER_ID);
    }
    
    /**
     * 初始化客户端
     */
    private static void initClient() {
        System.out.println("[初始化客户端]");
        rpcClient = Rpc.connect(ZK_ADDRESS);
        System.out.println("✓ 客户端已连接");
    }
    
    /**
     * 预热
     */
    private static void warmup() {
        IGameService service = rpcClient.get(IGameService.class, SERVER_ID);
        if (service == null) {
            throw new RuntimeException("无法获取服务代理");
        }
        
        long endTime = System.currentTimeMillis() + WARMUP_SECONDS * 1000;
        int count = 0;
        while (System.currentTimeMillis() < endTime) {
            try {
                service.getPlayerInfo(1L);
                count++;
            } catch (Exception e) {
                // 忽略预热错误
            }
        }
        System.out.println("预热请求数: " + count);
    }
    
    /**
     * 执行压力测试
     */
    private static TestResult runStressTest(int concurrency) throws Exception {
        IGameService service = rpcClient.get(IGameService.class, SERVER_ID);
        if (service == null) {
            throw new RuntimeException("无法获取服务代理");
        }
        
        // 统计信息
        AtomicLong successCount = new AtomicLong(0);
        AtomicLong failCount = new AtomicLong(0);
        List<Long> latencies = new CopyOnWriteArrayList<>();
        
        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch latch = new CountDownLatch(concurrency);
        
        long startTime = System.currentTimeMillis();
        long endTime = startTime + TEST_DURATION_SECONDS * 1000;
        
        // 提交任务
        for (int i = 0; i < concurrency; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    int requestCount = 0;
                    while (System.currentTimeMillis() < endTime) {
                        long requestStart = System.nanoTime();
                        try {
                            // 调用 RPC 方法
                            PlayerInfo result = service.getPlayerInfo((long) (threadId * 10000 + requestCount));
                            if (result != null) {
                                successCount.incrementAndGet();
                            } else {
                                failCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            failCount.incrementAndGet();
                        }
                        long latency = (System.nanoTime() - requestStart) / 1_000_000; // 转换为毫秒
                        latencies.add(latency);
                        requestCount++;
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待所有线程完成
        latch.await();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        long totalTime = System.currentTimeMillis() - startTime;
        long totalRequests = successCount.get() + failCount.get();
        
        // 计算统计信息
        TestResult result = new TestResult();
        result.concurrency = concurrency;
        result.totalRequests = totalRequests;
        result.successCount = successCount.get();
        result.failCount = failCount.get();
        result.successRate = totalRequests > 0 ? (double) successCount.get() / totalRequests : 0;
        result.qps = totalTime > 0 ? (totalRequests * 1000.0 / totalTime) : 0;
        result.totalTime = totalTime;
        
        // 计算延迟分布
        if (!latencies.isEmpty()) {
            List<Long> sorted = new ArrayList<>(latencies);
            sorted.sort(Long::compareTo);
            
            result.avgLatency = sorted.stream().mapToLong(Long::longValue).average().orElse(0);
            result.p50Latency = percentile(sorted, 0.5);
            result.p90Latency = percentile(sorted, 0.9);
            result.p95Latency = percentile(sorted, 0.95);
            result.p99Latency = percentile(sorted, 0.99);
            result.minLatency = sorted.get(0);
            result.maxLatency = sorted.get(sorted.size() - 1);
        }
        
        return result;
    }
    
    /**
     * 计算百分位数
     */
    private static long percentile(List<Long> sorted, double p) {
        if (sorted.isEmpty()) return 0;
        int index = (int) Math.ceil(sorted.size() * p) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }
    
    /**
     * 打印测试结果
     */
    private static void printResult(TestResult result) {
        System.out.println("│  总请求数:     " + String.format("%10d", result.totalRequests));
        System.out.println("│  成功数:       " + String.format("%10d", result.successCount));
        System.out.println("│  失败数:       " + String.format("%10d", result.failCount));
        System.out.println("│  成功率:       " + String.format("%10.2f%%", result.successRate * 100));
        System.out.println("│  QPS:          " + String.format("%10.2f", result.qps));
        System.out.println("│  总耗时:       " + String.format("%10d ms", result.totalTime));
        System.out.println("│");
        System.out.println("│  延迟统计 (ms):");
        System.out.println("│    平均:       " + String.format("%10.2f", result.avgLatency));
        System.out.println("│    P50:        " + String.format("%10d", result.p50Latency));
        System.out.println("│    P90:        " + String.format("%10d", result.p90Latency));
        System.out.println("│    P95:        " + String.format("%10d", result.p95Latency));
        System.out.println("│    P99:        " + String.format("%10d", result.p99Latency));
        System.out.println("│    最小:       " + String.format("%10d", result.minLatency));
        System.out.println("│    最大:       " + String.format("%10d", result.maxLatency));
    }
    
    /**
     * 清理资源
     */
    private static void cleanup() {
        System.out.println("[清理资源]");
        if (rpcClient != null) {
            rpcClient.shutdown();
        }
        if (gameServer != null) {
            gameServer.shutdown();
        }
        Rpc.shutdown();
        System.out.println("✓ 资源已清理");
    }
    
    /**
     * 测试结果
     */
    static class TestResult {
        int concurrency;
        long totalRequests;
        long successCount;
        long failCount;
        double successRate;
        double qps;
        long totalTime;
        double avgLatency;
        long p50Latency;
        long p90Latency;
        long p95Latency;
        long p99Latency;
        long minLatency;
        long maxLatency;
    }
}
