package com.muyi.game.player;

import com.muyi.game.playerdata.AbstractPlayerComponent;
import com.muyi.game.playerdata.AbstractPlayerManager;
import com.muyi.game.playerdata.PlayerDataContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 玩家消息执行器
 * <p>
 * 代表一个在线玩家的会话，持有 {@link PlayerToken}、消息推送能力和 {@link PlayerDataContext}。
 * 本身不持有线程资源，消息的线程调度由 {@link PlayerExecutorManager} 的条带化线程池负责。
 * <p>
 * 核心能力：
 * <ul>
 *   <li>数据管理 — 通过 {@link PlayerDataContext} 管理所有玩家数据</li>
 *   <li>认证校验 — 通过 token 验证消息来源合法性</li>
 *   <li>消息推送 — 通过 {@link GatePusher} 向玩家所在 gate 推送消息</li>
 *   <li>连接路由 — 通过 {@link PlayerToken} 知道玩家在哪个 gate/world</li>
 * </ul>
 */
public class PlayerExecutor {

    private static final Logger logger = LoggerFactory.getLogger(PlayerExecutor.class);

    private final long uid;
    private final PlayerToken token;
    private final GatePusher gatePusher;
    private PlayerDataContext dataContext;

    public PlayerExecutor(long uid, PlayerToken token, GatePusher gatePusher) {
        this.uid = uid;
        this.token = token;
        this.gatePusher = gatePusher;
    }

    // ==================== 认证 ====================

    /**
     * 验证认证凭据
     */
    public boolean validateAuth(String authToken) {
        return token.validateAuth(authToken);
    }

    // ==================== 推送 ====================

    /**
     * 向玩家推送消息（自动路由到正确的 gate）
     *
     * @param protoId 协议 ID
     * @param message 消息体
     */
    public void pushToGate(int protoId, Object message) {
        int gateServerId = token.getGateServerId();
        if (gateServerId <= 0) {
            logger.warn("Player[{}] has no gate connection, push dropped", uid);
            return;
        }
        try {
            gatePusher.push(gateServerId, uid, protoId, message);
        } catch (Exception e) {
            logger.error("Player[{}] push to gate-{} failed, protoId={}",
                    uid, gateServerId, protoId, e);
        }
    }

    // ==================== 活跃度 ====================

    /**
     * 刷新活跃时间（收到消息时调用）
     */
    public void refreshActivity() {
        token.refreshActivity();
    }

    /**
     * 是否超时未活跃
     */
    public boolean isIdle(long timeoutMs) {
        return token.isIdle(timeoutMs);
    }

    // ==================== Getters ====================

    public long getUid() {
        return uid;
    }

    public PlayerToken getToken() {
        return token;
    }

    public int getGateServerId() {
        return token.getGateServerId();
    }

    public int getWorldServerId() {
        return token.getWorldServerId();
    }

    // ==================== 玩家数据 ====================

    public PlayerDataContext getDataContext() {
        return dataContext;
    }

    public void setDataContext(PlayerDataContext dataContext) {
        this.dataContext = dataContext;
    }

    /**
     * 获取指定类型的组件（Manager 或 Logic）
     */
    public <C extends AbstractPlayerComponent> C getComponent(Class<C> clazz) {
        if (dataContext == null) {
            return null;
        }
        return dataContext.getComponent(clazz);
    }

    /**
     * 获取指定类型的 Manager（便捷方法）
     */
    public <M extends AbstractPlayerManager<?>> M getManager(Class<M> clazz) {
        if (dataContext == null) {
            return null;
        }
        return dataContext.getManager(clazz);
    }
}
