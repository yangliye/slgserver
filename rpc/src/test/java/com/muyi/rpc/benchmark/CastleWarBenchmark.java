package com.muyi.rpc.benchmark;

import com.muyi.rpc.Rpc;
import com.muyi.rpc.client.RpcProxyManager;
import com.muyi.rpc.server.RpcServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 王城战压测 - 模拟真实 SLG 峰值场景
 * 
 * 场景说明：
 * - 5000 玩家同时参战
 * - 每秒大量状态同步（位置、血量）
 * - 技能释放和伤害广播
 * - 跨服联盟支援
 * 
 * @author muyi
 */
public class CastleWarBenchmark {
    
    private static final String ZK_ADDRESS = "127.0.0.1:2181";
    
    // 王城战服务器配置
    private static final int BATTLE_SERVER_PORT = 18001;
    private static final int BATTLE_SERVER_ID = 1;
    
    // 游戏服务器配置（模拟多个游戏服）
    private static final int[] GAME_SERVER_PORTS = {18002, 18003, 18004};
    private static final int[] GAME_SERVER_IDS = {1, 2, 3};
    
    // 压测参数
    private static final int TOTAL_PLAYERS = 5000;           // 参战玩家数
    private static final int CONCURRENT_THREADS = 200;       // 并发线程数
    private static final int BATTLE_DURATION_SECONDS = 60;   // 战斗持续时间
    
    // 服务实例
    private static RpcServer battleServer;
    private static List<RpcServer> gameServers = new ArrayList<>();
    private static RpcProxyManager rpcClient;
    
    public static void main(String[] args) throws Exception {
        printBanner();
        
        // 启动服务
        startServers();
        Thread.sleep(2000);
        
        // 初始化客户端
        initClient();
        Thread.sleep(1000);
        
        // 预热
        warmup();
        
        // 执行压测
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  开始王城战压测");
        System.out.println("=".repeat(70) + "\n");
        
        // 阶段1: 玩家入场
        testPlayerEnter();
        
        // 阶段2: 战斗状态同步
        testBattleSync();
        
        // 阶段3: 技能释放峰值
        testSkillBurst();
        
        // 阶段4: 持续高负载战斗
        testSustainedBattle();
        
        // 阶段5: 跨服支援
        testCrossServerSupport();
        
        // 清理
        cleanup();
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  王城战压测完成");
        System.out.println("=".repeat(70) + "\n");
    }
    
    private static void printBanner() {
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    王城战压测 - Castle War Benchmark                ║");
        System.out.println("╠════════════════════════════════════════════════════════════════════╣");
        System.out.println("║  模拟场景:                                                          ║");
        System.out.println("║    - 5000 玩家同时参战                                              ║");
        System.out.println("║    - 每秒数万次状态同步                                             ║");
        System.out.println("║    - 技能释放峰值测试                                               ║");
        System.out.println("║    - 跨服联盟支援                                                   ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════╝");
        System.out.println();
    }
    
    // ==================== 服务启动 ====================
    
    private static void startServers() throws Exception {
        System.out.println("[启动服务器]");
        
        // 启动战场服务器
        battleServer = Rpc.server(BATTLE_SERVER_PORT)
                .serverId(BATTLE_SERVER_ID)
                .zookeeper(ZK_ADDRESS)
                .register(new BattleServiceImpl())
                .start();
        System.out.println("  ✓ 战场服务器已启动: port=" + BATTLE_SERVER_PORT);
        
        Thread.sleep(300);
        
        // 启动多个游戏服务器
        for (int i = 0; i < GAME_SERVER_PORTS.length; i++) {
            RpcServer gameServer = Rpc.server(GAME_SERVER_PORTS[i])
                    .serverId(GAME_SERVER_IDS[i])
                    .zookeeper(ZK_ADDRESS)
                    .register(new GameBattleServiceImpl(GAME_SERVER_IDS[i]))
                    .start();
            gameServers.add(gameServer);
            System.out.println("  ✓ 游戏服务器 " + GAME_SERVER_IDS[i] + " 已启动: port=" + GAME_SERVER_PORTS[i]);
            Thread.sleep(200);
        }
    }
    
    private static void initClient() {
        System.out.println("[初始化客户端]");
        rpcClient = Rpc.connect(ZK_ADDRESS);
        System.out.println("  ✓ 客户端已连接");
    }
    
    private static void warmup() {
        System.out.println("[预热中...]");
        IBattleService battleService = rpcClient.get(IBattleService.class, BATTLE_SERVER_ID);
        for (int i = 0; i < 1000; i++) {
            try {
                battleService.ping();
            } catch (Exception e) {
                // ignore
            }
        }
        System.out.println("  ✓ 预热完成");
    }
    
    // ==================== 测试阶段 ====================
    
    /**
     * 阶段1: 玩家入场
     * 模拟开战前 5000 玩家同时涌入战场
     */
    private static void testPlayerEnter() throws Exception {
        System.out.println("\n┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  阶段1: 玩家入场 (5000 玩家同时进入战场)                          │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘");
        
        IBattleService battleService = rpcClient.get(IBattleService.class, BATTLE_SERVER_ID);
        
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(TOTAL_PLAYERS);
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger fail = new AtomicInteger(0);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < TOTAL_PLAYERS; i++) {
            final long playerId = 100000 + i;
            final int allianceId = i % 10;  // 10 个联盟
            
            executor.submit(() -> {
                long reqStart = System.nanoTime();
                try {
                    battleService.enterBattle(playerId, allianceId);
                    success.incrementAndGet();
                    latencies.add((System.nanoTime() - reqStart) / 1_000_000);
                } catch (Exception e) {
                    fail.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        
        long totalTime = (System.nanoTime() - startTime) / 1_000_000;
        printResult("玩家入场", TOTAL_PLAYERS, success.get(), fail.get(), totalTime, latencies);
    }
    
    /**
     * 阶段2: 战斗状态同步
     * 模拟每个玩家每秒同步 2 次位置和状态
     */
    private static void testBattleSync() throws Exception {
        System.out.println("\n┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  阶段2: 状态同步 (5000玩家 x 2次/秒 = 10000 QPS, 持续10秒)        │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘");
        
        IBattleService battleService = rpcClient.get(IBattleService.class, BATTLE_SERVER_ID);
        
        int durationSeconds = 10;
        int syncPerSecond = 2;
        
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger fail = new AtomicInteger(0);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        
        long endTime = System.currentTimeMillis() + durationSeconds * 1000L;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(CONCURRENT_THREADS);
        
        long startTime = System.nanoTime();
        
        for (int t = 0; t < CONCURRENT_THREADS; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    int playersPerThread = TOTAL_PLAYERS / CONCURRENT_THREADS;
                    
                    while (System.currentTimeMillis() < endTime) {
                        for (int p = 0; p < playersPerThread; p++) {
                            long playerId = 100000 + threadId * playersPerThread + p;
                            int x = ThreadLocalRandom.current().nextInt(1000);
                            int y = ThreadLocalRandom.current().nextInt(1000);
                            int hp = ThreadLocalRandom.current().nextInt(10000);
                            
                            long reqStart = System.nanoTime();
                            try {
                                battleService.syncState(playerId, x, y, hp);
                                success.incrementAndGet();
                                latencies.add((System.nanoTime() - reqStart) / 1_000_000);
                            } catch (Exception e) {
                                fail.incrementAndGet();
                            }
                        }
                        // 控制同步频率
                        Thread.sleep(1000 / syncPerSecond);
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
        
        long totalTime = (System.nanoTime() - startTime) / 1_000_000;
        printResult("状态同步", success.get() + fail.get(), success.get(), fail.get(), totalTime, latencies);
    }
    
    /**
     * 阶段3: 技能释放峰值
     * 模拟集火时刻，大量技能同时释放
     */
    private static void testSkillBurst() throws Exception {
        System.out.println("\n┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  阶段3: 技能释放峰值 (模拟集火, 1秒内释放 10000 个技能)            │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘");
        
        IBattleService battleService = rpcClient.get(IBattleService.class, BATTLE_SERVER_ID);
        
        int totalSkills = 10000;
        
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(totalSkills);
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger fail = new AtomicInteger(0);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < totalSkills; i++) {
            final long attackerId = 100000 + ThreadLocalRandom.current().nextInt(TOTAL_PLAYERS);
            final long targetId = 100000 + ThreadLocalRandom.current().nextInt(TOTAL_PLAYERS);
            final int skillId = ThreadLocalRandom.current().nextInt(100);
            
            executor.submit(() -> {
                long reqStart = System.nanoTime();
                try {
                    battleService.useSkill(attackerId, targetId, skillId);
                    success.incrementAndGet();
                    latencies.add((System.nanoTime() - reqStart) / 1_000_000);
                } catch (Exception e) {
                    fail.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        long totalTime = (System.nanoTime() - startTime) / 1_000_000;
        printResult("技能释放", totalSkills, success.get(), fail.get(), totalTime, latencies);
    }
    
    /**
     * 阶段4: 持续高负载战斗
     * 模拟 30 秒持续战斗
     */
    private static void testSustainedBattle() throws Exception {
        System.out.println("\n┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  阶段4: 持续高负载战斗 (混合操作, 持续30秒)                        │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘");
        
        IBattleService battleService = rpcClient.get(IBattleService.class, BATTLE_SERVER_ID);
        
        int durationSeconds = 30;
        
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger fail = new AtomicInteger(0);
        AtomicLong syncCount = new AtomicLong(0);
        AtomicLong skillCount = new AtomicLong(0);
        AtomicLong queryCount = new AtomicLong(0);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        
        long endTime = System.currentTimeMillis() + durationSeconds * 1000L;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(CONCURRENT_THREADS);
        
        System.out.println("  运行中...");
        long startTime = System.nanoTime();
        
        for (int t = 0; t < CONCURRENT_THREADS; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    ThreadLocalRandom random = ThreadLocalRandom.current();
                    
                    while (System.currentTimeMillis() < endTime) {
                        long playerId = 100000 + random.nextInt(TOTAL_PLAYERS);
                        int op = random.nextInt(100);
                        
                        long reqStart = System.nanoTime();
                        try {
                            if (op < 60) {
                                // 60% 状态同步
                                battleService.syncState(playerId, random.nextInt(1000), 
                                        random.nextInt(1000), random.nextInt(10000));
                                syncCount.incrementAndGet();
                            } else if (op < 90) {
                                // 30% 技能释放
                                long targetId = 100000 + random.nextInt(TOTAL_PLAYERS);
                                battleService.useSkill(playerId, targetId, random.nextInt(100));
                                skillCount.incrementAndGet();
                            } else {
                                // 10% 查询战况
                                battleService.getBattleInfo();
                                queryCount.incrementAndGet();
                            }
                            success.incrementAndGet();
                            latencies.add((System.nanoTime() - reqStart) / 1_000_000);
                        } catch (Exception e) {
                            fail.incrementAndGet();
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
        
        long totalTime = (System.nanoTime() - startTime) / 1_000_000;
        
        System.out.println("  操作分布: 状态同步=" + syncCount.get() + 
                ", 技能释放=" + skillCount.get() + 
                ", 查询战况=" + queryCount.get());
        
        printResult("持续战斗", success.get() + fail.get(), success.get(), fail.get(), totalTime, latencies);
    }
    
    /**
     * 阶段5: 跨服支援
     * 模拟其他服务器的联盟成员赶来支援
     */
    private static void testCrossServerSupport() throws Exception {
        System.out.println("\n┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  阶段5: 跨服支援 (3个服务器各1000人支援, 共3000次跨服调用)         │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘");
        
        int playersPerServer = 1000;
        int totalPlayers = playersPerServer * GAME_SERVER_IDS.length;
        
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(totalPlayers);
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger fail = new AtomicInteger(0);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        
        long startTime = System.nanoTime();
        
        for (int serverId : GAME_SERVER_IDS) {
            IGameBattleService gameService = rpcClient.get(IGameBattleService.class, serverId);
            
            for (int i = 0; i < playersPerServer; i++) {
                final long playerId = serverId * 100000L + i;
                
                executor.submit(() -> {
                    long reqStart = System.nanoTime();
                    try {
                        // 跨服调用: 游戏服 -> 战场服
                        gameService.supportBattle(playerId, BATTLE_SERVER_ID);
                        success.incrementAndGet();
                        latencies.add((System.nanoTime() - reqStart) / 1_000_000);
                    } catch (Exception e) {
                        fail.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }
        
        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        
        long totalTime = (System.nanoTime() - startTime) / 1_000_000;
        printResult("跨服支援", totalPlayers, success.get(), fail.get(), totalTime, latencies);
    }
    
    // ==================== 辅助方法 ====================
    
    private static void printResult(String phase, int total, int success, int fail, 
                                    long totalTimeMs, List<Long> latencies) {
        double qps = totalTimeMs > 0 ? success * 1000.0 / totalTimeMs : 0;
        double successRate = total > 0 ? success * 100.0 / total : 0;
        
        System.out.println();
        System.out.println("  ┌────────────────────────────────────────────────────┐");
        System.out.printf("  │  %s 结果                                      │%n", phase);
        System.out.println("  ├────────────────────────────────────────────────────┤");
        System.out.printf("  │  总请求:    %,12d                            │%n", total);
        System.out.printf("  │  成功:      %,12d (%.1f%%)                    │%n", success, successRate);
        System.out.printf("  │  失败:      %,12d                            │%n", fail);
        System.out.printf("  │  耗时:      %,12d ms                         │%n", totalTimeMs);
        System.out.printf("  │  QPS:       %,12.0f                            │%n", qps);
        
        if (!latencies.isEmpty()) {
            List<Long> sorted = new ArrayList<>(latencies);
            Collections.sort(sorted);
            double avg = sorted.stream().mapToLong(Long::longValue).average().orElse(0);
            long p50 = percentile(sorted, 50);
            long p90 = percentile(sorted, 90);
            long p99 = percentile(sorted, 99);
            
            System.out.println("  ├────────────────────────────────────────────────────┤");
            System.out.printf("  │  平均延迟:  %,12.2f ms                         │%n", avg);
            System.out.printf("  │  P50:       %,12d ms                         │%n", p50);
            System.out.printf("  │  P90:       %,12d ms                         │%n", p90);
            System.out.printf("  │  P99:       %,12d ms                         │%n", p99);
        }
        System.out.println("  └────────────────────────────────────────────────────┘");
    }
    
    private static long percentile(List<Long> sorted, double p) {
        if (sorted.isEmpty()) return 0;
        int idx = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(idx, sorted.size() - 1)));
    }
    
    private static void cleanup() {
        System.out.println("\n[清理资源]");
        if (rpcClient != null) {
            rpcClient.shutdown();
        }
        for (RpcServer server : gameServers) {
            server.shutdown();
        }
        if (battleServer != null) {
            battleServer.shutdown();
        }
        Rpc.shutdown();
        System.out.println("  ✓ 资源已清理");
    }
}
