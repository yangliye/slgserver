package com.muyi.gate.session;

import com.muyi.common.util.time.TimeUtils;
import io.netty.channel.Channel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 玩家会话
 * 记录玩家当前状态和路由信息
 * 
 * 核心设计：跨服时只更新路由信息，不断开客户端连接
 *
 * @author muyi
 */
public class Session {
    
    /** 会话ID */
    private final String sessionId;
    
    /** 客户端连接（保持不变，跨服不断开） */
    private final Channel channel;
    
    /** 玩家ID（登录后设置） */
    private volatile long playerId;
    
    /** 账号 */
    private volatile String account;
    
    // ==================== 路由信息（跨服时更新这些，不断开连接）====================
    
    /** 当前所在 Game 服务器ID */
    private volatile int gameServerId;
    
    /** 联盟ID（用于联盟服务路由） */
    private volatile long allianceId;
    
    // ==================== 状态信息 ====================
    
    /** 会话状态 */
    private final AtomicReference<SessionState> state = new AtomicReference<>(SessionState.CONNECTED);
    
    /** 最后活跃时间 */
    private volatile long lastActiveTime;
    
    /** 登录时间 */
    private volatile long loginTime;
    
    /** 连接时间 */
    private final long connectTime;
    
    /** 客户端IP */
    private final String clientIp;
    
    /** 扩展属性 */
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    
    public Session(String sessionId, Channel channel, String clientIp) {
        this.sessionId = sessionId;
        this.channel = channel;
        this.clientIp = clientIp;
        this.connectTime = TimeUtils.currentTimeMillis();
        this.lastActiveTime = this.connectTime;
    }
    
    // ==================== 状态操作 ====================
    
    /**
     * 更新活跃时间
     */
    public void touch() {
        this.lastActiveTime = TimeUtils.currentTimeMillis();
    }
    
    /**
     * 设置为已认证状态
     */
    public boolean authenticate(long playerId, String account) {
        if (state.compareAndSet(SessionState.CONNECTED, SessionState.AUTHENTICATED)) {
            this.playerId = playerId;
            this.account = account;
            this.loginTime = TimeUtils.currentTimeMillis();
            return true;
        }
        return false;
    }
    
    /**
     * 进入游戏（绑定 Game 服务器 ID，地址由 Gate 通过 ZK 动态解析）
     */
    public boolean enterGame(int gameServerId) {
        if (state.compareAndSet(SessionState.AUTHENTICATED, SessionState.GAMING)) {
            this.gameServerId = gameServerId;
            return true;
        }
        return false;
    }
    
    /**
     * 开始迁服
     */
    public boolean startMigration() {
        return state.compareAndSet(SessionState.GAMING, SessionState.MIGRATING);
    }
    
    /**
     * 完成迁服（合服等场景切换 Game 服务器）
     */
    public boolean completeMigration(int newGameServerId) {
        if (state.compareAndSet(SessionState.MIGRATING, SessionState.GAMING)) {
            this.gameServerId = newGameServerId;
            return true;
        }
        return false;
    }
    
    /**
     * 取消迁服（回滚）
     */
    public boolean cancelMigration() {
        return state.compareAndSet(SessionState.MIGRATING, SessionState.GAMING);
    }
    
    /**
     * 断开连接
     */
    public void disconnect() {
        state.set(SessionState.OFFLINE);
    }
    
    /**
     * 是否可以发送消息到 Game 服务器
     */
    public boolean canRouteToGame() {
        SessionState current = state.get();
        return current == SessionState.GAMING && gameServerId > 0;
    }
    
    /**
     * 是否可以发送消息到 World 服务器
     */
    public boolean canRouteToWorld() {
        return canRouteToGame();
    }
    
    /**
     * 是否正在迁服
     */
    public boolean isMigrating() {
        return state.get() == SessionState.MIGRATING;
    }
    
    /**
     * 是否在线
     */
    public boolean isOnline() {
        SessionState current = state.get();
        return current != SessionState.OFFLINE && channel.isActive();
    }
    
    // ==================== 属性操作 ====================
    
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }
    
    public <T> T getAttribute(String key, T defaultValue) {
        Object value = attributes.get(key);
        return value != null ? (T) value : defaultValue;
    }
    
    public void removeAttribute(String key) {
        attributes.remove(key);
    }
    
    // ==================== Getter ====================
    
    public String getSessionId() {
        return sessionId;
    }
    
    public Channel getChannel() {
        return channel;
    }
    
    public long getPlayerId() {
        return playerId;
    }
    
    public String getAccount() {
        return account;
    }
    
    public int getGameServerId() {
        return gameServerId;
    }
    
    public long getAllianceId() {
        return allianceId;
    }
    
    public void setAllianceId(long allianceId) {
        this.allianceId = allianceId;
    }
    
    public SessionState getState() {
        return state.get();
    }
    
    public long getLastActiveTime() {
        return lastActiveTime;
    }
    
    public long getLoginTime() {
        return loginTime;
    }
    
    public long getConnectTime() {
        return connectTime;
    }
    
    public String getClientIp() {
        return clientIp;
    }
    
    @Override
    public String toString() {
        return "Session{" +
                "sessionId='" + sessionId + '\'' +
                ", playerId=" + playerId +
                ", gameServerId=" + gameServerId +
                ", state=" + state.get() +
                ", clientIp='" + clientIp + '\'' +
                '}';
    }
}
