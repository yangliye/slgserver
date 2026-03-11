package com.muyi.world.player;

import com.muyi.core.thread.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * World 侧玩家执行器管理器
 * <p>
 * 与 Game 侧的 PlayerExecutorManager 同一模式：
 * 条带化线程池保证同一玩家消息串行，不同玩家并行。
 */
public class WorldExecutorManager {

    private static final Logger log = LoggerFactory.getLogger(WorldExecutorManager.class);

    private static final String POOL_PREFIX = "world-stripe-";

    private final int stripeCount;
    private final ExecutorService[] stripes;
    private final WorldGatePusher gatePusher;

    private final ConcurrentHashMap<Long, WorldExecutor> players = new ConcurrentHashMap<>();

    @SuppressWarnings("resource")
    public WorldExecutorManager(ThreadPoolManager poolManager, int stripeCount, WorldGatePusher gatePusher) {
        this.stripeCount = stripeCount;
        this.gatePusher = gatePusher;
        this.stripes = new ExecutorService[stripeCount];
        for (int i = 0; i < stripeCount; i++) {
            stripes[i] = poolManager.newSinglePool(POOL_PREFIX + i);
        }
        log.info("WorldExecutorManager created with {} stripes", stripeCount);
    }

    // ==================== 绑定 / 解绑 ====================

    /**
     * 玩家进入大世界
     */
    public WorldExecutor bind(long uid, int gameServerId, int gateServerId) {
        WorldExecutor executor = new WorldExecutor(uid, gameServerId, gateServerId, gatePusher);
        WorldExecutor old = players.put(uid, executor);
        if (old != null) {
            log.info("Player[{}] rebind in world", uid);
        }
        log.debug("Player[{}] entered world, game={}, gate={}, online={}",
                uid, gameServerId, gateServerId, players.size());
        return executor;
    }

    /**
     * 玩家离开大世界
     */
    public boolean unbind(long uid) {
        WorldExecutor executor = players.remove(uid);
        if (executor == null) {
            return false;
        }
        log.debug("Player[{}] left world, online={}", uid, players.size());
        return true;
    }

    // ==================== 消息投递 ====================

    /**
     * 投递到玩家对应的条带线程
     */
    public void dispatch(long uid, Runnable task) {
        stripes[stripeIndex(uid)].execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                log.error("Player[{}] world task error", uid, e);
            }
        });
    }

    // ==================== 查询 ====================

    public WorldExecutor get(long uid) {
        return players.get(uid);
    }

    public boolean isOnline(long uid) {
        return players.containsKey(uid);
    }

    public int getOnlineCount() {
        return players.size();
    }

    public Map<Long, WorldExecutor> snapshot() {
        return Map.copyOf(players);
    }

    // ==================== 生命周期 ====================

    public void shutdown() {
        log.info("WorldExecutorManager shutdown, {} players online", players.size());
        players.clear();
    }

    private int stripeIndex(long uid) {
        return (int) (Math.abs(uid) % stripeCount);
    }
}
