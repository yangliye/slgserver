package com.muyi.gate.session;

import com.muyi.common.util.time.TimeUtils;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 会话管理器
 * 管理所有客户端连接的会话
 *
 * @author muyi
 */
public class SessionManager {
    
    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);
    
    /** Channel -> Session */
    private final Map<Channel, Session> sessionByChannel = new ConcurrentHashMap<>();
    
    /** SessionId -> Session */
    private final Map<String, Session> sessionById = new ConcurrentHashMap<>();
    
    /** PlayerId -> Session（登录后才有） */
    private final Map<Long, Session> sessionByPlayerId = new ConcurrentHashMap<>();
    
    /**
     * 创建会话
     */
    public Session createSession(Channel channel, String clientIp) {
        String sessionId = generateSessionId();
        Session session = new Session(sessionId, channel, clientIp);
        
        sessionByChannel.put(channel, session);
        sessionById.put(sessionId, session);
        
        log.info("Session created: {} from {}", sessionId, clientIp);
        return session;
    }
    
    /**
     * 绑定玩家ID到会话
     */
    public void bindPlayerId(Session session, long playerId) {
        // 检查是否已有其他会话
        Session existingSession = sessionByPlayerId.get(playerId);
        if (existingSession != null && existingSession != session) {
            // 踢掉旧的连接
            log.info("Kicking existing session for player: {}", playerId);
            removeSession(existingSession);
            existingSession.getChannel().close();
        }
        
        sessionByPlayerId.put(playerId, session);
        log.info("Player {} bound to session {}", playerId, session.getSessionId());
    }
    
    /**
     * 移除会话
     */
    public void removeSession(Session session) {
        if (session == null) {
            return;
        }
        
        session.disconnect();
        sessionByChannel.remove(session.getChannel());
        sessionById.remove(session.getSessionId());
        
        if (session.getPlayerId() > 0) {
            sessionByPlayerId.remove(session.getPlayerId());
        }
        
        log.info("Session removed: {} (player={})", session.getSessionId(), session.getPlayerId());
    }
    
    /**
     * 移除会话（通过 Channel）
     */
    public Session removeSession(Channel channel) {
        Session session = sessionByChannel.get(channel);
        if (session != null) {
            removeSession(session);
        }
        return session;
    }
    
    /**
     * 获取会话（通过 Channel）
     */
    public Session getSession(Channel channel) {
        return sessionByChannel.get(channel);
    }
    
    /**
     * 获取会话（通过 SessionId）
     */
    public Session getSession(String sessionId) {
        return sessionById.get(sessionId);
    }
    
    /**
     * 获取会话（通过 PlayerId）
     */
    public Session getSessionByPlayerId(long playerId) {
        return sessionByPlayerId.get(playerId);
    }
    
    /**
     * 获取所有会话
     */
    public Collection<Session> getAllSessions() {
        return sessionById.values();
    }
    
    /**
     * 获取指定 Game 服务器的所有会话
     */
    public Collection<Session> getSessionsByGameServerId(int gameServerId) {
        return sessionByPlayerId.values().stream()
                .filter(s -> s.getGameServerId() == gameServerId)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取指定 World 服务器的所有会话
     */
    public Collection<Session> getSessionsByWorldServerId(int worldServerId) {
        return sessionByPlayerId.values().stream()
                .filter(s -> s.getWorldServerId() == worldServerId)
                .collect(Collectors.toList());
    }
    
    /**
     * @deprecated 使用 getSessionsByGameServerId() 或 getSessionsByWorldServerId()
     */
    @Deprecated
    public Collection<Session> getSessionsByServerId(int serverId) {
        return getSessionsByGameServerId(serverId);
    }
    
    /**
     * 获取连接数
     */
    public int getConnectionCount() {
        return sessionByChannel.size();
    }
    
    /**
     * 获取在线玩家数
     */
    public int getOnlinePlayerCount() {
        return sessionByPlayerId.size();
    }
    
    /**
     * 清理超时会话
     */
    public int cleanupTimeoutSessions(long timeoutMs) {
        long now = TimeUtils.currentTimeMillis();
        int count = 0;
        
        for (Session session : sessionByChannel.values()) {
            // 只清理未登录的超时连接
            if (session.getPlayerId() == 0 && now - session.getLastActiveTime() > timeoutMs) {
                log.info("Cleaning up timeout session: {}", session.getSessionId());
                removeSession(session);
                session.getChannel().close();
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * 生成会话ID
     */
    private String generateSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
