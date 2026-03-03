package com.muyi.game.player;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 玩家消息执行器
 * <p>
 * 代表一个在线玩家的会话，持有 {@link PlayerToken} 和消息推送能力。
 * 本身不持有线程资源，消息的线程调度由 {@link PlayerExecutorManager} 的条带化线程池负责。
 * <p>
 * 核心能力：
 * <ul>
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
}
