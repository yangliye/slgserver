package com.muyi.gate.service;

import com.muyi.gate.api.IGateService;
import com.muyi.gate.migrate.MigrationRequest;
import com.muyi.gate.migrate.MigrationResult;
import com.muyi.gate.migrate.ServerMigrator;
import com.muyi.gate.session.Session;
import com.muyi.gate.session.SessionManager;
import com.muyi.rpc.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Gate 服务实现
 *
 * @author muyi
 */
@RpcService(IGateService.class)
public class GateServiceImpl implements IGateService {
    
    private static final Logger log = LoggerFactory.getLogger(GateServiceImpl.class);
    
    private final SessionManager sessionManager;
    private final ServerMigrator serverMigrator;
    
    public GateServiceImpl(SessionManager sessionManager, ServerMigrator serverMigrator) {
        this.sessionManager = sessionManager;
        this.serverMigrator = serverMigrator;
    }
    
    @Override
    public boolean pushMessage(long playerId, int protoId, Object message) {
        Session session = sessionManager.getSessionByPlayerId(playerId);
        if (session == null || !session.isOnline()) {
            log.debug("Player {} is offline, cannot push message", playerId);
            return false;
        }
        
        // TODO: 序列化并发送消息
        // session.getChannel().writeAndFlush(encodeMessage(protoId, message));
        log.debug("Pushed message to player {}: protoId={}", playerId, protoId);
        return true;
    }
    
    @Override
    public int pushMessageToPlayers(long[] playerIds, int protoId, Object message) {
        int count = 0;
        for (long playerId : playerIds) {
            if (pushMessage(playerId, protoId, message)) {
                count++;
            }
        }
        return count;
    }
    
    @Override
    public int broadcast(int protoId, Object message) {
        int count = 0;
        for (Session session : sessionManager.getAllSessions()) {
            if (session.isOnline() && session.getPlayerId() > 0) {
                // TODO: 序列化并发送消息
                count++;
            }
        }
        log.info("Broadcast message protoId={} to {} players", protoId, count);
        return count;
    }
    
    @Override
    public int broadcastToServer(int serverId, int protoId, Object message) {
        Collection<Session> sessions = sessionManager.getSessionsByServerId(serverId);
        int count = 0;
        for (Session session : sessions) {
            if (session.isOnline()) {
                // TODO: 序列化并发送消息
                count++;
            }
        }
        log.info("Broadcast message protoId={} to {} players on server {}", protoId, count, serverId);
        return count;
    }
    
    @Override
    public boolean kickPlayer(long playerId, String reason) {
        Session session = sessionManager.getSessionByPlayerId(playerId);
        if (session == null) {
            return false;
        }
        
        log.info("Kicking player {}: {}", playerId, reason);
        
        // TODO: 发送踢出消息给客户端
        // pushMessage(playerId, KICK_PROTO_ID, new KickMessage(reason));
        
        // 关闭连接
        session.getChannel().close();
        sessionManager.removeSession(session);
        
        return true;
    }
    
    @Override
    public boolean isOnline(long playerId) {
        Session session = sessionManager.getSessionByPlayerId(playerId);
        return session != null && session.isOnline();
    }
    
    @Override
    public int getPlayerServerId(long playerId) {
        Session session = sessionManager.getSessionByPlayerId(playerId);
        if (session != null && session.isOnline()) {
            return session.getServerId();
        }
        return -1;
    }
    
    @Override
    public MigrationResult migratePlayer(MigrationRequest request) {
        try {
            // 同步等待迁服完成
            return serverMigrator.migrate(request).get();
        } catch (Exception e) {
            log.error("Migration failed for player {}", request.getPlayerId(), e);
            return MigrationResult.fail(request.getPlayerId(), 3001, "迁服执行异常");
        }
    }
    
    @Override
    public int getOnlineCount() {
        return sessionManager.getOnlinePlayerCount();
    }
    
    @Override
    public int getOnlineCountByServer(int serverId) {
        return sessionManager.getSessionsByServerId(serverId).size();
    }
}
