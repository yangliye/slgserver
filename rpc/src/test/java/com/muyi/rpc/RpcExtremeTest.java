package com.muyi.rpc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.muyi.rpc.core.RpcFuture;
import com.muyi.rpc.game.GameServer;
import com.muyi.rpc.game.GameServerConfig;
import com.muyi.rpc.gamei.IGameService;
import com.muyi.rpc.world.WorldServer;
import com.muyi.rpc.world.WorldServerConfig;

/**
 * RPC 极限压力测试
 * 测试系统在极端负载下的表现
 *
 * @author muyi
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RpcExtremeTest {

    private static final String ZK_ADDRESS = "127.0.0.1:2181";

    private static WorldServer worldServer;
    private static GameServer gameServer1;
    private static GameServer gameServer2;

    @BeforeAll
    static void setUp() throws Exception {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  RPC EXTREME STRESS TEST - 极限压力测试");
        System.out.println("=".repeat(70) + "\n");

        // 启动服务器
        System.out.println("[Setup] Starting servers...");
        
        WorldServerConfig worldConfig = new WorldServerConfig()
                .serverId(1).port(17001).zkAddress(ZK_ADDRESS);
        worldServer = new WorldServer(worldConfig);
        worldServer.start();

        Thread.sleep(300);

        GameServerConfig gameConfig1 = new GameServerConfig()
                .serverId(1).port(17002).zkAddress(ZK_ADDRESS);
        gameServer1 = new GameServer(gameConfig1);
        gameServer1.start();

        Thread.sleep(200);

        GameServerConfig gameConfig2 = new GameServerConfig()
                .serverId(2).port(17003).zkAddress(ZK_ADDRESS);
        gameServer2 = new GameServer(gameConfig2);
        gameServer2.start();

        Thread.sleep(500);

        // 预热：创建玩家数据
        System.out.println("[Warmup] Creating 1000 test players...");
        for (int i = 0; i < 1000; i++) {
            gameServer2.login(100000 + i, "warmup");
        }
        System.out.println("[Setup] All servers ready!\n");
    }

    @AfterAll
    static void tearDown() {
        System.out.println("\n[Cleanup] Shutting down...");
        if (gameServer2 != null) gameServer2.shutdown();
        if (gameServer1 != null) gameServer1.shutdown();
        if (worldServer != null) worldServer.shutdown();
        System.out.println("[Cleanup] Done.\n");
    }

    @Test
    @Order(1)
    void testExtremeConcurrency() throws Exception {
        System.out.println("=".repeat(70));
        System.out.println("  TEST 1: EXTREME CONCURRENCY (极限并发测试)");
        System.out.println("=".repeat(70));
        System.out.println("  并发线程: 200/500/1000");
        System.out.println("  每线程请求: 100");
        System.out.println("-".repeat(70) + "\n");

        IGameService game2Service = gameServer1.getGameService(2);

        int[] threadCounts = {200, 500, 1000};
        int requestsPerThread = 100;

        for (int threads : threadCounts) {
            System.out.println(">>> Testing with " + threads + " concurrent threads...");
            StressResult result = runConcurrentTest(game2Service, threads, requestsPerThread);
            printResult(threads + " threads", result);
        }
    }

    @Test
    @Order(2)
    void testMassiveRequests() throws Exception {
        System.out.println("=".repeat(70));
        System.out.println("  TEST 2: MASSIVE REQUESTS (海量请求测试)");
        System.out.println("=".repeat(70));
        System.out.println("  固定并发: 100 线程");
        System.out.println("  总请求量: 10万/50万/100万");
        System.out.println("-".repeat(70) + "\n");

        IGameService game2Service = gameServer1.getGameService(2);

        int threads = 100;
        int[] totalRequests = {100_000, 500_000, 1_000_000};

        for (int total : totalRequests) {
            int perThread = total / threads;
            System.out.println(">>> Testing " + formatNumber(total) + " requests...");
            StressResult result = runConcurrentTest(game2Service, threads, perThread);
            printResult(formatNumber(total) + " reqs", result);
        }
    }

    @Test
    @Order(3)
    void testSustainedHighLoad() throws Exception {
        System.out.println("=".repeat(70));
        System.out.println("  TEST 3: SUSTAINED HIGH LOAD (持续高负载测试)");
        System.out.println("=".repeat(70));
        System.out.println("  并发线程: 100");
        System.out.println("  持续时间: 30 秒");
        System.out.println("-".repeat(70) + "\n");

        IGameService game2Service = gameServer1.getGameService(2);

        int threads = 100;
        int durationSeconds = 30;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicLong totalLatency = new AtomicLong(0);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());

        long endTime = System.currentTimeMillis() + durationSeconds * 1000L;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threads);

        System.out.println(">>> Running for " + durationSeconds + " seconds...");
        long startTime = System.nanoTime();

        for (int t = 0; t < threads; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    while (System.currentTimeMillis() < endTime) {
                        long playerId = 100000 + ThreadLocalRandom.current().nextInt(1000);
                        long reqStart = System.nanoTime();
                        try {
                            game2Service.getPlayerInfo(playerId);
                            long latency = (System.nanoTime() - reqStart) / 1_000_000;
                            latencies.add(latency);
                            totalLatency.addAndGet(latency);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            failCount.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await(durationSeconds + 30, TimeUnit.SECONDS);
        executor.shutdown();

        long actualDuration = (System.nanoTime() - startTime) / 1_000_000;
        int totalRequests = successCount.get() + failCount.get();

        StressResult result = new StressResult(totalRequests, successCount.get(), 
                failCount.get(), actualDuration, latencies);
        printResult("30s sustained", result);
        printPercentiles(latencies);
    }

    @Test
    @Order(4)
    void testAsyncFlood() throws Exception {
        System.out.println("=".repeat(70));
        System.out.println("  TEST 4: ASYNC FLOOD (异步洪水测试)");
        System.out.println("=".repeat(70));
        System.out.println("  一次性发送大量异步请求，测试系统承压能力");
        System.out.println("-".repeat(70) + "\n");

        IGameService game2Service = gameServer1.getGameService(2);

        int[] requestCounts = {5000, 10000, 20000};

        for (int total : requestCounts) {
            System.out.println(">>> Flooding " + formatNumber(total) + " async requests...");
            
            CountDownLatch latch = new CountDownLatch(total);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);
            List<Long> latencies = Collections.synchronizedList(new ArrayList<>());

            long startTime = System.nanoTime();

            for (int i = 0; i < total; i++) {
                long playerId = 100000 + (i % 1000);
                long reqStart = System.nanoTime();

                try {
                    RpcFuture future = game2Service.deductGoldAsync(playerId, 1);
                    future.onSuccess(r -> {
                        long latency = (System.nanoTime() - reqStart) / 1_000_000;
                        latencies.add(latency);
                        successCount.incrementAndGet();
                        latch.countDown();
                    }).onFail(e -> {
                        failCount.incrementAndGet();
                        latch.countDown();
                    });
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    latch.countDown();
                }
            }

            boolean completed = latch.await(120, TimeUnit.SECONDS);
            long totalTime = (System.nanoTime() - startTime) / 1_000_000;

            if (!completed) {
                System.out.println("    WARNING: Timeout! Some requests did not complete.");
            }

            StressResult result = new StressResult(total, successCount.get(), 
                    failCount.get(), totalTime, latencies);
            printResult(formatNumber(total) + " async", result);
        }
    }

    @Test
    @Order(5)
    void testFindQpsLimit() throws Exception {
        System.out.println("=".repeat(70));
        System.out.println("  TEST 5: FIND QPS LIMIT (探测 QPS 极限)");
        System.out.println("=".repeat(70));
        System.out.println("  逐步增加并发，找到 QPS 峰值");
        System.out.println("-".repeat(70) + "\n");

        IGameService game2Service = gameServer1.getGameService(2);

        int[] threadCounts = {50, 100, 200, 300, 400, 500, 600, 800, 1000};
        int requestsPerThread = 200;
        double maxQps = 0;
        int bestThreads = 0;

        System.out.printf("%-12s %-12s %-12s %-12s %-12s%n", 
                "Threads", "QPS", "Avg(ms)", "P99(ms)", "Success%");
        System.out.println("-".repeat(60));

        for (int threads : threadCounts) {
            StressResult result = runConcurrentTest(game2Service, threads, requestsPerThread);
            
            double p99 = 0;
            if (!result.latencies.isEmpty()) {
                List<Long> sorted = new ArrayList<>(result.latencies);
                Collections.sort(sorted);
                int idx = (int) Math.ceil(0.99 * sorted.size()) - 1;
                p99 = sorted.get(Math.max(0, idx));
            }

            System.out.printf("%-12d %-12.0f %-12.2f %-12.0f %-12.1f%n",
                    threads, result.qps(), result.avgLatency(), p99, result.successRate());

            if (result.qps() > maxQps) {
                maxQps = result.qps();
                bestThreads = threads;
            }

            // 如果成功率开始下降，可以提前停止
            if (result.successRate() < 95) {
                System.out.println("    (Stopping: success rate dropped below 95%)");
                break;
            }
        }

        System.out.println("-".repeat(60));
        System.out.printf("Peak QPS: %.0f @ %d threads%n%n", maxQps, bestThreads);
    }

    // ==================== Helper Methods ====================

    private StressResult runConcurrentTest(IGameService service, int threads, int requestsPerThread) 
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());

        long startTime = System.nanoTime();

        for (int t = 0; t < threads; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < requestsPerThread; i++) {
                        long playerId = 100000 + ThreadLocalRandom.current().nextInt(1000);
                        long reqStart = System.nanoTime();
                        try {
                            service.getPlayerInfo(playerId);
                            long latency = (System.nanoTime() - reqStart) / 1_000_000;
                            latencies.add(latency);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            failCount.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await(300, TimeUnit.SECONDS);
        executor.shutdown();

        long totalTime = (System.nanoTime() - startTime) / 1_000_000;
        int totalRequests = threads * requestsPerThread;

        return new StressResult(totalRequests, successCount.get(), failCount.get(), totalTime, latencies);
    }

    private void printResult(String label, StressResult result) {
        System.out.println();
        System.out.println("+" + "-".repeat(58) + "+");
        System.out.printf("| %-56s |%n", label);
        System.out.println("+" + "-".repeat(58) + "+");
        System.out.printf("| Total:     %,12d requests                         |%n", result.totalRequests);
        System.out.printf("| Success:   %,12d (%.1f%%)                           |%n", 
                result.successCount, result.successRate());
        System.out.printf("| Failed:    %,12d                                   |%n", result.failCount);
        System.out.printf("| Duration:  %,12d ms                                |%n", result.totalTimeMs);
        System.out.printf("| QPS:       %,12.0f req/s                            |%n", result.qps());
        if (!result.latencies.isEmpty()) {
            System.out.printf("| Avg Lat:   %,12.2f ms                               |%n", result.avgLatency());
            System.out.printf("| Min Lat:   %,12d ms                                |%n", result.minLatency());
            System.out.printf("| Max Lat:   %,12d ms                                |%n", result.maxLatency());
        }
        System.out.println("+" + "-".repeat(58) + "+");
        System.out.println();
    }

    private void printPercentiles(List<Long> latencies) {
        if (latencies.isEmpty()) return;
        
        List<Long> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);

        System.out.println("  Latency Percentiles:");
        System.out.printf("    P50:  %4d ms%n", percentile(sorted, 50));
        System.out.printf("    P90:  %4d ms%n", percentile(sorted, 90));
        System.out.printf("    P95:  %4d ms%n", percentile(sorted, 95));
        System.out.printf("    P99:  %4d ms%n", percentile(sorted, 99));
        System.out.printf("    P999: %4d ms%n", percentile(sorted, 99.9));
        System.out.println();
    }

    private long percentile(List<Long> sorted, double p) {
        int idx = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, idx));
    }

    private String formatNumber(int num) {
        if (num >= 1_000_000) return (num / 1_000_000) + "M";
        if (num >= 1_000) return (num / 1_000) + "K";
        return String.valueOf(num);
    }

    private static class StressResult {
        final int totalRequests;
        final int successCount;
        final int failCount;
        final long totalTimeMs;
        final List<Long> latencies;

        StressResult(int total, int success, int fail, long time, List<Long> latencies) {
            this.totalRequests = total;
            this.successCount = success;
            this.failCount = fail;
            this.totalTimeMs = time;
            this.latencies = latencies;
        }

        double successRate() {
            return totalRequests > 0 ? successCount * 100.0 / totalRequests : 0;
        }

        double qps() {
            return totalTimeMs > 0 ? successCount * 1000.0 / totalTimeMs : 0;
        }

        double avgLatency() {
            return latencies.isEmpty() ? 0 : 
                    latencies.stream().mapToLong(Long::longValue).average().orElse(0);
        }

        long minLatency() {
            return latencies.isEmpty() ? 0 : 
                    latencies.stream().mapToLong(Long::longValue).min().orElse(0);
        }

        long maxLatency() {
            return latencies.isEmpty() ? 0 : 
                    latencies.stream().mapToLong(Long::longValue).max().orElse(0);
        }
    }
}
