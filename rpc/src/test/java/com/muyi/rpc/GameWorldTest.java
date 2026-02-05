package com.muyi.rpc;

import com.muyi.rpc.client.RpcProxyManager;
import com.muyi.rpc.core.RpcFuture;
import com.muyi.rpc.game.GameServer;
import com.muyi.rpc.game.GameServerConfig;
import com.muyi.rpc.gamei.IGameService;
import com.muyi.rpc.gamei.DeductResult;
import com.muyi.rpc.gamei.PlayerInfo;
import com.muyi.rpc.gamei.TransferResult;
import com.muyi.rpc.worldi.WorldEnterResult;
import com.muyi.rpc.world.WorldServer;
import com.muyi.rpc.world.WorldServerConfig;
import org.junit.jupiter.api.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Game-World 服务互调测试
 * 模拟真实游戏场景：
 * - GameServer -> WorldServer RPC 调用
 * - GameServer -> GameServer 跨服 RPC 调用
 * 
 * 服务通过 ZooKeeper 自动发现，无需硬编码地址
 *
 * @author muyi
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GameWorldTest {
    
    private static final String ZK_ADDRESS = "127.0.0.1:2181";
    
    // 服务器实例
    private static WorldServer worldServer;
    private static GameServer gameServer1;  // Game-1
    private static GameServer gameServer2;  // Game-2
    
    @BeforeAll
    static void setUp() throws Exception {
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║           Game-World RPC Test - Startup                        ║");
        System.out.println("╠════════════════════════════════════════════════════════════════╣");
        System.out.println("║  服务通过 ZooKeeper 自动发现，无需硬编码地址                      ║");
        System.out.println("║                                                                ║");
        System.out.println("║    [Game-1] ◄──RPC──► [Game-2]                                ║");
        System.out.println("║        │                  │                                    ║");
        System.out.println("║        └──────► [World] ◄─┘                                    ║");
        System.out.println("║                                                                ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        // =====================================================
        // 1. 启动 World Server (使用简化 API)
        // =====================================================
        System.out.println("[Step 1] Starting World Server...");
        
        WorldServerConfig worldConfig = new WorldServerConfig()
                .serverId(1)
                .port(19001)
                .zkAddress(ZK_ADDRESS);
        
        worldServer = new WorldServer(worldConfig);
        worldServer.start();
        System.out.println("         ✓ World Server started");
        
        Thread.sleep(500);
        
        // =====================================================
        // 2. 启动 Game Server 1
        // =====================================================
        System.out.println("\n[Step 2] Starting Game Server 1...");
        
        GameServerConfig gameConfig1 = new GameServerConfig()
                .serverId(1)
                .port(19002)
                .zkAddress(ZK_ADDRESS);
        
        gameServer1 = new GameServer(gameConfig1);
        gameServer1.start();
        System.out.println("         ✓ Game Server 1 started");
        
        Thread.sleep(300);
        
        // =====================================================
        // 3. 启动 Game Server 2
        // =====================================================
        System.out.println("\n[Step 3] Starting Game Server 2...");
        
        GameServerConfig gameConfig2 = new GameServerConfig()
                .serverId(2)
                .port(19003)
                .zkAddress(ZK_ADDRESS);
        
        gameServer2 = new GameServer(gameConfig2);
        gameServer2.start();
        System.out.println("         ✓ Game Server 2 started");
        
        Thread.sleep(300);
        
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println();
    }
    
    @AfterAll
    static void tearDown() {
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("[Cleanup] Shutting down...");
        
        if (gameServer2 != null) {
            gameServer2.shutdown();
            System.out.println("         ✓ Game Server 2 stopped");
        }
        
        if (gameServer1 != null) {
            gameServer1.shutdown();
            System.out.println("         ✓ Game Server 1 stopped");
        }
        
        if (worldServer != null) {
            worldServer.shutdown();
            System.out.println("         ✓ World Server stopped");
        }
        
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println();
    }
    
    // =====================================================
    // 测试用例
    // =====================================================
    
    @Test
    @Order(1)
    void testPlayerLogin() {
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  Test 1: Player Login                                          │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        
        PlayerInfo player = gameServer1.login(10001, "test_token");
        
        assertNotNull(player);
        assertEquals(10001, player.getPlayerId());
        
        System.out.println("│  Request:  gameServer1.login(10001, \"test_token\")              │");
        System.out.println("│  Response: " + player);
        System.out.println("│  ✓ Login successful                                            │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘");
        System.out.println();
    }
    
    @Test
    @Order(2)
    void testLoginAndEnterWorld() {
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  Test 2: Login and Enter World (Game -> World RPC)             │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        
        long playerId = 10002;
        int worldId = 1;
        
        WorldEnterResult result = gameServer1.loginAndEnterWorld(playerId, "world_token", worldId);
        
        assertNotNull(result);
        assertTrue(result.isSuccess(), "Should enter world successfully");
        assertNotNull(result.getWorldInfo());
        assertEquals(worldId, result.getWorldInfo().getWorldId());
        
        System.out.println("│  Request:  loginAndEnterWorld(" + playerId + ", token, " + worldId + ")");
        System.out.println("│  Response: " + result.getMessage());
        System.out.println("│  World:    " + result.getWorldInfo());
        System.out.println("│  ✓ Game -> World RPC successful                                │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘");
        System.out.println();
    }
    
    @Test
    @Order(3)
    void testCrossServerTransfer() {
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  Test 3: Cross-Server Gold Transfer (Game-1 -> Game-2 RPC)     │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  场景: 玩家A在Game-1, 玩家B在Game-2, A向B转账                     │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        
        // 1. 在 Game-1 登录玩家A
        long playerA = 50001;
        PlayerInfo playerInfoA = gameServer1.login(playerA, "tokenA");
        assertNotNull(playerInfoA);
        long initialGoldA = playerInfoA.getGold();
        System.out.println("│  [Game-1] Player A: " + playerA + ", Gold: " + initialGoldA);
        
        // 2. 在 Game-2 登录玩家B
        long playerB = 50002;
        PlayerInfo playerInfoB = gameServer2.login(playerB, "tokenB");
        assertNotNull(playerInfoB);
        long initialGoldB = playerInfoB.getGold();
        System.out.println("│  [Game-2] Player B: " + playerB + ", Gold: " + initialGoldB);
        
        // 3. 从 Game-1 向 Game-2 的玩家B转账
        long transferAmount = 100;
        int targetServerId = 2;  // Game-2 的 serverId
        
        System.out.println("│  Transfer: " + transferAmount + " gold from A to B (cross-server)");
        
        TransferResult result = gameServer1.crossServerTransfer(playerA, playerB, transferAmount, targetServerId);
        
        assertNotNull(result);
        assertTrue(result.isSuccess(), "Transfer should succeed");
        assertEquals(playerA, result.getFromPlayerId());
        assertEquals(playerB, result.getToPlayerId());
        assertEquals(transferAmount, result.getAmount());
        
        // 4. 验证余额变化
        PlayerInfo afterA = gameServer1.getPlayerInfo(playerA);
        PlayerInfo afterB = gameServer2.getPlayerInfo(playerB);
        
        assertEquals(initialGoldA - transferAmount, afterA.getGold(), "Player A gold should decrease");
        assertEquals(initialGoldB + transferAmount, afterB.getGold(), "Player B gold should increase");
        
        System.out.println("│  Result:  " + result.getMessage());
        System.out.println("│  [Game-1] Player A Gold: " + initialGoldA + " -> " + afterA.getGold());
        System.out.println("│  [Game-2] Player B Gold: " + initialGoldB + " -> " + afterB.getGold());
        System.out.println("│  ✓ Cross-server transfer successful                            │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘");
        System.out.println();
    }
    
    @Test
    @Order(4)
    void testCrossServerTransferInsufficientGold() {
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  Test 4: Cross-Server Transfer - Insufficient Gold             │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        
        // 1. 在 Game-1 登录玩家C（余额1000）
        long playerC = 60001;
        PlayerInfo playerInfoC = gameServer1.login(playerC, "tokenC");
        assertNotNull(playerInfoC);
        System.out.println("│  [Game-1] Player C Gold: " + playerInfoC.getGold());
        
        // 2. 在 Game-2 登录玩家D
        long playerD = 60002;
        gameServer2.login(playerD, "tokenD");
        
        // 3. 尝试转账超过余额的金额
        long transferAmount = 9999;
        TransferResult result = gameServer1.crossServerTransfer(playerC, playerD, transferAmount, 2);
        
        assertNotNull(result);
        assertFalse(result.isSuccess(), "Transfer should fail due to insufficient gold");
        
        System.out.println("│  Attempt: Transfer " + transferAmount + " gold (exceeds balance)");
        System.out.println("│  Result:  " + result.getMessage());
        System.out.println("│  ✓ Insufficient gold check works                               │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘");
        System.out.println();
    }
    
    @Test
    @Order(5)
    void testAsyncCrossServerTransfer() throws Exception {
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  Test 5: Async Cross-Server Deduct (异步跨服扣款)               │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  场景: Player1(Game-1) 异步RPC让Player2(Game-2)扣100金币给自己   │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        
        // 1. 在 Game-1 登录 Player1
        long player1Id = 70001;
        PlayerInfo player1 = gameServer1.login(player1Id, "token1");
        assertNotNull(player1);
        long player1InitialGold = player1.getGold();
        System.out.println("│  [Game-1] Player1: " + player1Id + ", 初始金币: " + player1InitialGold);
        
        // 2. 在 Game-2 登录 Player2
        long player2Id = 70002;
        PlayerInfo player2 = gameServer2.login(player2Id, "token2");
        assertNotNull(player2);
        long player2InitialGold = player2.getGold();
        System.out.println("│  [Game-2] Player2: " + player2Id + ", 初始金币: " + player2InitialGold);
        
        // 3. Player1 异步 RPC 调用 Game-2，让 Player2 扣除 100 金币
        long deductAmount = 100;
        int targetServerId = 2;
        
        System.out.println("│");
        System.out.println("│  >>> Player1 发起异步 RPC 调用 Game-2...");
        System.out.println("│      请求: Player2 扣除 " + deductAmount + " 金币");
        
        // 获取 Game-2 的服务代理
        IGameService game2Service = gameServer1.getGameService(targetServerId);
        
        // 使用 CountDownLatch 等待异步结果
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<DeductResult> resultRef = new AtomicReference<>();
        
        // 异步调用 - 让 Player2 扣除金币
        RpcFuture future = game2Service.deductGoldAsync(player2Id, deductAmount);
        
        // 注册回调 - 收到结果后 Player1 增加金币
        future.onSuccess(result -> {
            DeductResult deductResult = (DeductResult) result;
            System.out.println("│  <<< 收到异步响应!");
            System.out.println("│      扣款结果: " + deductResult.getMessage());
            System.out.println("│      扣除金额: " + deductResult.getDeductedAmount());
            System.out.println("│      Player2 剩余: " + deductResult.getRemainingBalance());
            
            if (deductResult.isSuccess()) {
                // Player1 增加金币（本地操作）
                PlayerInfo p1 = gameServer1.getPlayerInfo(player1Id);
                long newGold = p1.getGold() + deductResult.getDeductedAmount();
                p1.setGold(newGold);
                System.out.println("│");
                System.out.println("│  >>> Player1 增加 " + deductResult.getDeductedAmount() + " 金币");
                System.out.println("│      Player1 剩余金币: " + newGold);
            }
            
            resultRef.set(deductResult);
            latch.countDown();
        }).onFail(error -> {
            System.out.println("│  <<< 异步调用失败: " + error.getMessage());
            latch.countDown();
        });
        
        System.out.println("│  ... Player1 主线程继续执行其他操作（模拟异步）...");
        
        // 等待异步结果（最多 10 秒）
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertTrue(completed, "异步调用应该在超时前完成");
        
        DeductResult result = resultRef.get();
        assertNotNull(result);
        assertTrue(result.isSuccess(), "扣款应该成功: " + (result != null ? result.getMessage() : "null"));
        
        // 4. 验证余额变化
        PlayerInfo player1After = gameServer1.getPlayerInfo(player1Id);
        PlayerInfo player2After = gameServer2.getPlayerInfo(player2Id);
        
        assertEquals(player1InitialGold + deductAmount, player1After.getGold(), 
                "Player1 金币应该增加");
        assertEquals(player2InitialGold - deductAmount, player2After.getGold(), 
                "Player2 金币应该减少");
        
        System.out.println("│");
        System.out.println("│  ═══════════════════════════════════════════════════════════════");
        System.out.println("│  最终结果:");
        System.out.println("│    [Game-1] Player1: " + player1InitialGold + " → " + player1After.getGold() + " (+100)");
        System.out.println("│    [Game-2] Player2: " + player2InitialGold + " → " + player2After.getGold() + " (-100)");
        System.out.println("│  ═══════════════════════════════════════════════════════════════");
        System.out.println("│");
        System.out.println("│  ✓ 异步跨服扣款成功! Player1 获得了 Player2 的 100 金币          │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘");
        System.out.println();
    }
    
    @Test
    @Order(6)
    void testMultiplePlayersEnterWorld() {
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  Test 6: Multiple Players Enter World                          │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        
        int playerCount = 20;
        int worldId = 2;
        int successCount = 0;
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < playerCount; i++) {
            long playerId = 20000 + i;
            WorldEnterResult result = gameServer1.loginAndEnterWorld(playerId, "batch_token", worldId);
            if (result != null && result.isSuccess()) {
                successCount++;
            }
        }
        
        long costTime = System.currentTimeMillis() - startTime;
        
        assertEquals(playerCount, successCount, "All players should enter successfully");
        
        System.out.println("│  Players:  " + playerCount);
        System.out.println("│  Success:  " + successCount);
        System.out.println("│  Time:     " + costTime + "ms (" + (costTime / playerCount) + "ms/player)");
        System.out.println("│  ✓ Batch enter world successful                                │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘");
        System.out.println();
    }
    
    @Test
    @Order(7)
    void testSimpleApiUsage() {
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  Test 7: Simple API Usage Demo                                 │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│                                                                │");
        System.out.println("│  // 一行启动服务端                                               │");
        System.out.println("│  RpcServer server = Rpc.serve(8001, 1, \"localhost:2181\",        │");
        System.out.println("│                                gameService, worldService);      │");
        System.out.println("│                                                                │");
        System.out.println("│  // 一行获取客户端                                               │");
        System.out.println("│  RpcProxyManager rpc = Rpc.connect(\"localhost:2181\");           │");
        System.out.println("│  IGameService game = rpc.get(IGameService.class, 1);           │");
        System.out.println("│                                                                │");
        System.out.println("│  // 高级配置（Builder模式）                                       │");
        System.out.println("│  Rpc.server(8001)                                              │");
        System.out.println("│     .serverId(1).zookeeper(\"...\").weight(200).serve(...);     │");
        System.out.println("│                                                                │");
        System.out.println("│  ✓ API 简洁，一行代码即可启动                                    │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘");
        System.out.println();
    }
}
