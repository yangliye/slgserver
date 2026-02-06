package com.muyi.gate.migrate;

import com.muyi.common.util.time.TimeUtils;
import com.muyi.gate.session.Session;
import com.muyi.gate.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 服务器迁移器
 * 处理玩家跨服/迁服逻辑，实现无缝切换
 * 
 * 架构说明：
 * - Gate 同时连接 Game（个人数据）和 World（世界地图）
 * - 跨服主要是 World ↔ World，Game 数据通常不变
 * 
 * 迁移类型：
 * - WORLD：只切换 World 服务器（最常见，如跨区域、跨服战）
 * - GAME：只切换 Game 服务器（少见）
 * - FULL：Game 和 World 都切换（如合服）
 * 
 * World 迁服流程（最常见）：
 * 1. Gate 收到迁服请求
 * 2. 设置 Session 为 MIGRATING 状态（暂停 World 消息转发，Game 消息正常）
 * 3. 通知源 World 服务器保存玩家在该区域的数据
 * 4. 通知目标 World 服务器加载/初始化玩家数据
 * 5. 更新 Session 的 World 路由地址
 * 6. 设置 Session 为 GAMING 状态（恢复消息转发）
 * 
 * 关键点：客户端连接始终保持，只是切换了消息路由目标
 *
 * @author muyi
 */
public class ServerMigrator {
    
    private static final Logger log = LoggerFactory.getLogger(ServerMigrator.class);
    
    /** 迁服超时时间（毫秒） */
    private static final long MIGRATION_TIMEOUT_MS = 30000;
    
    private final SessionManager sessionManager;
    
    /** 进行中的迁服任务 */
    private final Map<Long, MigrationTask> migrationTasks = new ConcurrentHashMap<>();
    
    public ServerMigrator(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }
    
    /**
     * 执行迁服
     * 
     * @param request 迁服请求
     * @return 迁服结果（异步）
     */
    public CompletableFuture<MigrationResult> migrate(MigrationRequest request) {
        long playerId = request.getPlayerId();
        
        // 1. 检查是否已有进行中的迁服
        if (migrationTasks.containsKey(playerId)) {
            return CompletableFuture.completedFuture(
                    MigrationResult.fail(playerId, 1001, "迁服进行中，请勿重复操作"));
        }
        
        // 2. 获取 Session
        Session session = sessionManager.getSessionByPlayerId(playerId);
        if (session == null) {
            return CompletableFuture.completedFuture(
                    MigrationResult.fail(playerId, 1002, "玩家不在线"));
        }
        
        // 3. 根据迁移类型检查状态
        MigrationType type = request.getType();
        if (type == MigrationType.WORLD || type == MigrationType.FULL) {
            if (!session.canRouteToWorld()) {
                return CompletableFuture.completedFuture(
                        MigrationResult.fail(playerId, 1003, "玩家当前状态不允许 World 迁服"));
            }
        }
        if (type == MigrationType.GAME || type == MigrationType.FULL) {
            if (!session.canRouteToGame()) {
                return CompletableFuture.completedFuture(
                        MigrationResult.fail(playerId, 1003, "玩家当前状态不允许 Game 迁服"));
            }
        }
        
        // 4. 开始迁服
        if (!session.startMigration()) {
            return CompletableFuture.completedFuture(
                    MigrationResult.fail(playerId, 1004, "无法开始迁服"));
        }
        
        // 5. 创建迁服任务
        MigrationTask task = new MigrationTask(request, session);
        migrationTasks.put(playerId, task);
        
        log.info("Starting {} migration for player {}: World {} -> {}, Game {} -> {}", 
                type, playerId, 
                request.getSourceWorldServerId(), request.getTargetWorldServerId(),
                request.getSourceGameServerId(), request.getTargetGameServerId());
        
        // 6. 异步执行迁服流程
        return executeMigration(task)
                .orTimeout(MIGRATION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .whenComplete((result, ex) -> {
                    migrationTasks.remove(playerId);
                    if (ex != null) {
                        log.error("Migration failed for player {}: {}", playerId, ex.getMessage());
                        session.cancelMigration();
                    }
                });
    }
    
    /**
     * 执行迁服流程
     */
    private CompletableFuture<MigrationResult> executeMigration(MigrationTask task) {
        MigrationRequest request = task.getRequest();
        Session session = task.getSession();
        long playerId = request.getPlayerId();
        MigrationType type = request.getType();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 根据迁移类型执行不同的流程
                switch (type) {
                    case WORLD:
                        return executeWorldMigration(request, session);
                    case GAME:
                        return executeGameMigration(request, session);
                    case FULL:
                        return executeFullMigration(request, session);
                    default:
                        session.cancelMigration();
                        return MigrationResult.fail(playerId, 2000, "未知迁移类型");
                }
            } catch (Exception e) {
                log.error("Migration error for player {}", playerId, e);
                session.cancelMigration();
                return MigrationResult.fail(playerId, 2099, "迁服异常: " + e.getMessage());
            }
        });
    }
    
    /**
     * 执行 World 迁移（最常见）
     * Game 数据不动，只切换 World 服务器
     */
    private MigrationResult executeWorldMigration(MigrationRequest request, Session session) {
        long playerId = request.getPlayerId();
        
        // Step 1: 通知源 World 服务器保存玩家区域数据
        log.debug("World Migration Step 1: Saving player region data on source World server");
        boolean saved = notifySourceWorldSave(request);
        if (!saved) {
            session.cancelMigration();
            return MigrationResult.fail(playerId, 2001, "World 数据保存失败");
        }
        
        // Step 2: 通知目标 World 服务器加载/初始化玩家数据
        log.debug("World Migration Step 2: Loading player data on target World server");
        boolean loaded = notifyTargetWorldLoad(request);
        if (!loaded) {
            session.cancelMigration();
            return MigrationResult.fail(playerId, 2002, "World 数据加载失败");
        }
        
        // Step 3: 更新 Session 的 World 路由信息
        log.debug("World Migration Step 3: Updating World route");
        boolean updated = session.completeWorldMigration(
                request.getTargetWorldServerId(),
                request.getTargetWorldServerAddress());
        
        if (!updated) {
            session.cancelMigration();
            return MigrationResult.fail(playerId, 2003, "更新 World 路由失败");
        }
        
        // Step 4: 通知客户端
        log.debug("World Migration Step 4: Notifying client");
        notifyClientMigrationComplete(session, request.getTargetWorldServerId());
        
        log.info("World migration completed for player {}: World {} -> {}", 
                playerId, request.getSourceWorldServerId(), request.getTargetWorldServerId());
        
        return MigrationResult.success(playerId, request.getTargetWorldServerId());
    }
    
    /**
     * 执行 Game 迁移（少见）
     */
    private MigrationResult executeGameMigration(MigrationRequest request, Session session) {
        long playerId = request.getPlayerId();
        
        // Step 1: 通知源 Game 服务器保存数据
        log.debug("Game Migration Step 1: Saving player data on source Game server");
        boolean saved = notifySourceGameSave(request);
        if (!saved) {
            session.cancelMigration();
            return MigrationResult.fail(playerId, 2001, "Game 数据保存失败");
        }
        
        // Step 2: 通知目标 Game 服务器加载数据
        log.debug("Game Migration Step 2: Loading player data on target Game server");
        boolean loaded = notifyTargetGameLoad(request);
        if (!loaded) {
            session.cancelMigration();
            return MigrationResult.fail(playerId, 2002, "Game 数据加载失败");
        }
        
        // Step 3: 更新 Session（Game 迁移通常 World 也要跟着变）
        log.debug("Game Migration Step 3: Updating routes");
        boolean updated = session.completeGameMigration(
                request.getTargetGameServerId(),
                request.getTargetGameServerAddress(),
                request.getTargetWorldServerId(),
                request.getTargetWorldServerAddress());
        
        if (!updated) {
            session.cancelMigration();
            return MigrationResult.fail(playerId, 2003, "更新路由失败");
        }
        
        log.info("Game migration completed for player {}: Game {} -> {}", 
                playerId, request.getSourceGameServerId(), request.getTargetGameServerId());
        
        return MigrationResult.success(playerId, request.getTargetGameServerId());
    }
    
    /**
     * 执行完整迁移（Game + World）
     */
    private MigrationResult executeFullMigration(MigrationRequest request, Session session) {
        long playerId = request.getPlayerId();
        
        // Step 1: 保存 Game 数据
        log.debug("Full Migration Step 1: Saving Game data");
        if (!notifySourceGameSave(request)) {
            session.cancelMigration();
            return MigrationResult.fail(playerId, 2001, "Game 数据保存失败");
        }
        
        // Step 2: 保存 World 数据
        log.debug("Full Migration Step 2: Saving World data");
        if (!notifySourceWorldSave(request)) {
            session.cancelMigration();
            return MigrationResult.fail(playerId, 2001, "World 数据保存失败");
        }
        
        // Step 3: 加载目标 Game 数据
        log.debug("Full Migration Step 3: Loading Game data");
        if (!notifyTargetGameLoad(request)) {
            session.cancelMigration();
            return MigrationResult.fail(playerId, 2002, "Game 数据加载失败");
        }
        
        // Step 4: 加载目标 World 数据
        log.debug("Full Migration Step 4: Loading World data");
        if (!notifyTargetWorldLoad(request)) {
            session.cancelMigration();
            return MigrationResult.fail(playerId, 2002, "World 数据加载失败");
        }
        
        // Step 5: 更新所有路由
        log.debug("Full Migration Step 5: Updating all routes");
        boolean updated = session.completeGameMigration(
                request.getTargetGameServerId(),
                request.getTargetGameServerAddress(),
                request.getTargetWorldServerId(),
                request.getTargetWorldServerAddress());
        
        if (!updated) {
            session.cancelMigration();
            return MigrationResult.fail(playerId, 2003, "更新路由失败");
        }
        
        // Step 6: 通知客户端
        notifyClientMigrationComplete(session, request.getTargetWorldServerId());
        
        log.info("Full migration completed for player {}: Game {} -> {}, World {} -> {}", 
                playerId, 
                request.getSourceGameServerId(), request.getTargetGameServerId(),
                request.getSourceWorldServerId(), request.getTargetWorldServerId());
        
        return MigrationResult.success(playerId, request.getTargetWorldServerId());
    }
    
    // ==================== World 服务器通知 ====================
    
    /**
     * 通知源 World 服务器保存玩家区域数据
     * TODO: 实现 RPC 调用 IWorldService.savePlayerForMigration(playerId)
     */
    private boolean notifySourceWorldSave(MigrationRequest request) {
        log.debug("Notifying source World server {} to save player {} region data", 
                request.getSourceWorldServerId(), request.getPlayerId());
        
        // TODO: 实现 RPC 调用
        // rpcClient.call(request.getSourceWorldServerAddress(), IWorldService.class)
        //         .savePlayerForMigration(request.getPlayerId());
        
        simulateDelay(100);
        return true;
    }
    
    /**
     * 通知目标 World 服务器加载玩家数据
     * TODO: 实现 RPC 调用 IWorldService.loadPlayerForMigration(playerId, sourceWorldServerId)
     */
    private boolean notifyTargetWorldLoad(MigrationRequest request) {
        log.debug("Notifying target World server {} to load player {} data", 
                request.getTargetWorldServerId(), request.getPlayerId());
        
        // TODO: 实现 RPC 调用
        simulateDelay(100);
        return true;
    }
    
    // ==================== Game 服务器通知 ====================
    
    /**
     * 通知源 Game 服务器保存玩家数据
     * TODO: 实现 RPC 调用 IGameService.savePlayerForMigration(playerId)
     */
    private boolean notifySourceGameSave(MigrationRequest request) {
        log.debug("Notifying source Game server {} to save player {} data", 
                request.getSourceGameServerId(), request.getPlayerId());
        
        // TODO: 实现 RPC 调用
        simulateDelay(100);
        return true;
    }
    
    /**
     * 通知目标 Game 服务器加载玩家数据
     * TODO: 实现 RPC 调用 IGameService.loadPlayerForMigration(playerId)
     */
    private boolean notifyTargetGameLoad(MigrationRequest request) {
        log.debug("Notifying target Game server {} to load player {} data", 
                request.getTargetGameServerId(), request.getPlayerId());
        
        // TODO: 实现 RPC 调用
        simulateDelay(100);
        return true;
    }
    
    // ==================== 客户端通知 ====================
    
    /**
     * 通知客户端迁服完成
     */
    private void notifyClientMigrationComplete(Session session, int newServerId) {
        // 通过 session.getChannel() 发送迁服完成消息给客户端
        // 客户端可以选择刷新界面或显示提示
        log.debug("Notifying client about migration complete: player {} now on server {}", 
                session.getPlayerId(), newServerId);
    }
    
    /**
     * 模拟网络延迟（临时用于测试）
     */
    private void simulateDelay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 取消迁服
     */
    public boolean cancelMigration(long playerId) {
        MigrationTask task = migrationTasks.get(playerId);
        if (task != null) {
            task.getSession().cancelMigration();
            migrationTasks.remove(playerId);
            log.info("Migration cancelled for player {}", playerId);
            return true;
        }
        return false;
    }
    
    /**
     * 获取迁服中的玩家数量
     */
    public int getMigratingCount() {
        return migrationTasks.size();
    }
    
    /**
     * 检查玩家是否正在迁服
     */
    public boolean isMigrating(long playerId) {
        return migrationTasks.containsKey(playerId);
    }
    
    /**
     * 迁服任务
     */
    private static class MigrationTask {
        private final MigrationRequest request;
        private final Session session;
        private final long startTime;
        
        public MigrationTask(MigrationRequest request, Session session) {
            this.request = request;
            this.session = session;
            this.startTime = TimeUtils.currentTimeMillis();
        }
        
        public MigrationRequest getRequest() {
            return request;
        }
        
        public Session getSession() {
            return session;
        }
        
        public long getStartTime() {
            return startTime;
        }
    }
}
