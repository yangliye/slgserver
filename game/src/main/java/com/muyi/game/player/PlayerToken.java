package com.muyi.game.player;

import com.muyi.common.util.time.TimeUtils;

/**
 * 玩家会话令牌
 * <p>
 * 维护玩家在服务器集群中的连接路由信息：
 * <ul>
 *   <li>通过哪个 gate 连接（用于推送消息给客户端）</li>
 *   <li>所在哪个 world（用于跨服逻辑）</li>
 *   <li>认证凭据（用于消息校验）</li>
 * </ul>
 * <p>
 * gate/world 连接信息在玩家跨服、重连时可变更。
 */
public class PlayerToken {

    private final long uid;
    private volatile String authToken;

    /** 玩家连接的 gate 服务器 ID */
    private volatile int gateServerId;
    /** 玩家所在 world 服务器 ID（未进入 world 时为 0） */
    private volatile int worldServerId;

    private final long loginTime;
    private volatile long lastActiveTime;

    public PlayerToken(long uid, String authToken, int gateServerId) {
        this.uid = uid;
        this.authToken = authToken;
        this.gateServerId = gateServerId;
        this.loginTime = TimeUtils.currentTimeMillis();
        this.lastActiveTime = this.loginTime;
    }

    /**
     * 验证认证凭据
     */
    public boolean validateAuth(String authToken) {
        return this.authToken != null && this.authToken.equals(authToken);
    }

    /**
     * 刷新活跃时间（收到消息时调用）
     */
    public void refreshActivity() {
        this.lastActiveTime = TimeUtils.currentTimeMillis();
    }

    /**
     * 是否超时未活跃
     *
     * @param timeoutMs 超时毫秒数
     */
    public boolean isIdle(long timeoutMs) {
        return TimeUtils.currentTimeMillis() - lastActiveTime > timeoutMs;
    }

    // ==================== 连接路由变更 ====================

    /**
     * 重连刷新（新凭据 + 可能换了 gate）
     */
    public void refreshToken(String newAuthToken, int newGateServerId) {
        this.authToken = newAuthToken;
        this.gateServerId = newGateServerId;
        this.lastActiveTime = TimeUtils.currentTimeMillis();
    }

    /**
     * 变更 gate 连接（重连到不同 gate 时）
     */
    public void setGateServerId(int gateServerId) {
        this.gateServerId = gateServerId;
    }

    /**
     * 进入/离开 world
     *
     * @param worldServerId world 服务器 ID，离开时传 0
     */
    public void setWorldServerId(int worldServerId) {
        this.worldServerId = worldServerId;
    }

    // ==================== Getters ====================

    public long getUid() {
        return uid;
    }

    public String getAuthToken() {
        return authToken;
    }

    public int getGateServerId() {
        return gateServerId;
    }

    public int getWorldServerId() {
        return worldServerId;
    }

    public long getLoginTime() {
        return loginTime;
    }

    public long getLastActiveTime() {
        return lastActiveTime;
    }

    @Override
    public String toString() {
        return "PlayerToken{uid=" + uid
                + ", gate=" + gateServerId
                + ", world=" + worldServerId + '}';
    }
}
