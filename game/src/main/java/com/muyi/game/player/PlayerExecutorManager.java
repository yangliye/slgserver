package com.muyi.game.player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.muyi.core.thread.ThreadPoolManager;

/**
 * 玩家执行器管理器
 * <p>
 * 管理所有在线玩家的 {@link PlayerExecutor}，负责消息调度和推送。
 * <p>
 * 采用条带化线程池（Striped Executor）：
 * <ul>
 *   <li>通过 {@link ThreadPoolManager} 创建 N 个单线程池作为条带</li>
 *   <li>按 uid % N 路由，同一玩家的消息始终分配到同一个线程</li>
 *   <li>保证单个玩家消息有序，不同玩家消息并行</li>
 *   <li>无论在线人数多少，线程数恒定，无资源浪费</li>
 * </ul>
 *
 * <pre>{@code
 * // 初始化
 * GatePusher pusher = (gateId, playerId, protoId, msg) ->
 *     getRpcProxy().get(IGateService.class, gateId).pushMessage(playerId, protoId, msg);
 * PlayerExecutorManager mgr = new PlayerExecutorManager(poolManager, 8, pusher);
 *
 * // 玩家登录
 * mgr.bind(uid, authToken, gateServerId);
 *
 * // gate 转发消息
 * mgr.submit(uid, authToken, () -> handleMessage(uid, msg));
 *
 * // 主动推送给玩家
 * mgr.get(uid).pushToGate(protoId, message);
 *
 * // 玩家下线
 * mgr.unbind(uid);
 * }</pre>
 */
public class PlayerExecutorManager {

    private static final Logger logger = LoggerFactory.getLogger(PlayerExecutorManager.class);

    private static final String POOL_PREFIX = "player-stripe-";

    private final int stripeCount;
    private final ExecutorService[] stripes;
    private final GatePusher gatePusher;

    private final ConcurrentHashMap<Long, PlayerExecutor> players = new ConcurrentHashMap<>();

    /**
     * @param poolManager 模块线程池管理器
     * @param stripeCount 条带数（建议 CPU 核心数或根据负载调整）
     * @param gatePusher  gate 推送实现
     */
    @SuppressWarnings("resource")
    public PlayerExecutorManager(ThreadPoolManager poolManager, int stripeCount, GatePusher gatePusher) {
        this.stripeCount = stripeCount;
        this.gatePusher = gatePusher;
        this.stripes = new ExecutorService[stripeCount];
        for (int i = 0; i < stripeCount; i++) {
            stripes[i] = poolManager.newSinglePool(POOL_PREFIX + i);
        }
        logger.info("PlayerExecutorManager created with {} stripes", stripeCount);
    }

    // ==================== 绑定 / 解绑 ====================

    /**
     * 绑定玩家（登录时调用）
     * <p>
     * 重连或顶号时自动替换旧实例。
     *
     * @param uid          玩家 ID
     * @param authToken    认证凭据
     * @param gateServerId 玩家连接的 gate 服务器 ID
     * @return 新创建的执行器
     */
    public PlayerExecutor bind(long uid, String authToken, int gateServerId) {
        PlayerToken token = new PlayerToken(uid, authToken, gateServerId);
        PlayerExecutor pe = new PlayerExecutor(uid, token, gatePusher);

        PlayerExecutor old = players.put(uid, pe);
        if (old != null) {
            logger.info("Player[{}] rebind (reconnect/kick)", uid);
        }
        logger.debug("Player[{}] bound to stripe-{}, gate={}, online={}",
                uid, stripeIndex(uid), gateServerId, players.size());
        return pe;
    }

    /**
     * 解绑玩家（下线时调用）
     */
    public boolean unbind(long uid) {
        PlayerExecutor pe = players.remove(uid);
        if (pe == null) {
            return false;
        }
        logger.debug("Player[{}] unbound, online={}", uid, players.size());
        return true;
    }

    // ==================== 消息投递 ====================

    /**
     * 向指定玩家提交消息（带认证校验）
     *
     * @param uid       玩家 ID
     * @param authToken 请求携带的认证凭据
     * @param task      消息处理逻辑
     * @return 是否成功提交
     */
    public boolean submit(long uid, String authToken, Runnable task) {
        PlayerExecutor pe = players.get(uid);
        if (pe == null) {
            logger.warn("Player[{}] not online, message dropped", uid);
            return false;
        }
        if (!pe.validateAuth(authToken)) {
            logger.warn("Player[{}] auth mismatch, message dropped", uid);
            return false;
        }
        pe.refreshActivity();
        dispatch(uid, task);
        return true;
    }

    /**
     * 向指定玩家提交消息（无校验，内部调用使用）
     */
    public void dispatch(long uid, Runnable task) {
        stripes[stripeIndex(uid)].execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                logger.error("Player[{}] task error", uid, e);
            }
        });
    }

    /**
     * 向所有在线玩家广播任务
     */
    public void broadcast(BiConsumer<Long, PlayerExecutor> task) {
        for (var entry : players.entrySet()) {
            long uid = entry.getKey();
            PlayerExecutor pe = entry.getValue();
            dispatch(uid, () -> task.accept(uid, pe));
        }
    }

    // ==================== 推送 ====================

    /**
     * 向指定玩家推送消息（通过 gate 到客户端）
     *
     * @return 是否成功（玩家不在线返回 false）
     */
    public boolean pushToPlayer(long uid, int protoId, Object message) {
        PlayerExecutor pe = players.get(uid);
        if (pe == null) {
            return false;
        }
        pe.pushToGate(protoId, message);
        return true;
    }

    /**
     * 向所有在线玩家推送消息（通过各自的 gate）
     */
    public void pushToAll(int protoId, Object message) {
        for (PlayerExecutor pe : players.values()) {
            pe.pushToGate(protoId, message);
        }
    }

    // ==================== 查询 ====================

    public PlayerExecutor get(long uid) {
        return players.get(uid);
    }

    public boolean isOnline(long uid) {
        return players.containsKey(uid);
    }

    public int getOnlineCount() {
        return players.size();
    }

    public Map<Long, PlayerExecutor> snapshot() {
        return Map.copyOf(players);
    }

    // ==================== 生命周期 ====================

    /**
     * 清除所有玩家状态（线程池生命周期由 ThreadPoolManager 管理）
     */
    public void shutdown() {
        logger.info("PlayerExecutorManager shutdown, {} players online", players.size());
        players.clear();
    }

    // ==================== Internal ====================

    private int stripeIndex(long uid) {
        return (int) (Math.abs(uid) % stripeCount);
    }
}
