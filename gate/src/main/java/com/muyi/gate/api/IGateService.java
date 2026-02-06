package com.muyi.gate.api;

import com.muyi.gate.migrate.MigrationRequest;
import com.muyi.gate.migrate.MigrationResult;

/**
 * Gate 服务 RPC 接口
 * 供其他模块调用的网关服务
 *
 * @author muyi
 */
public interface IGateService {
    
    /**
     * 推送消息给指定玩家
     * 
     * @param playerId 玩家ID
     * @param protoId 协议ID
     * @param message 消息内容
     * @return 是否成功
     */
    boolean pushMessage(long playerId, int protoId, Object message);
    
    /**
     * 推送消息给多个玩家
     * 
     * @param playerIds 玩家ID列表
     * @param protoId 协议ID
     * @param message 消息内容
     * @return 成功推送的数量
     */
    int pushMessageToPlayers(long[] playerIds, int protoId, Object message);
    
    /**
     * 广播消息给所有在线玩家
     * 
     * @param protoId 协议ID
     * @param message 消息内容
     * @return 成功推送的数量
     */
    int broadcast(int protoId, Object message);
    
    /**
     * 广播消息给指定服务器的所有玩家
     * 
     * @param serverId 服务器ID
     * @param protoId 协议ID
     * @param message 消息内容
     * @return 成功推送的数量
     */
    int broadcastToServer(int serverId, int protoId, Object message);
    
    /**
     * 踢掉玩家
     * 
     * @param playerId 玩家ID
     * @param reason 踢出原因
     * @return 是否成功
     */
    boolean kickPlayer(long playerId, String reason);
    
    /**
     * 检查玩家是否在线
     * 
     * @param playerId 玩家ID
     * @return 是否在线
     */
    boolean isOnline(long playerId);
    
    /**
     * 获取玩家所在服务器ID
     * 
     * @param playerId 玩家ID
     * @return 服务器ID，不在线返回 -1
     */
    int getPlayerServerId(long playerId);
    
    /**
     * 执行玩家迁服
     * 
     * @param request 迁服请求
     * @return 迁服结果
     */
    MigrationResult migratePlayer(MigrationRequest request);
    
    /**
     * 获取在线玩家数量
     * 
     * @return 在线数量
     */
    int getOnlineCount();
    
    /**
     * 获取指定服务器的在线玩家数量
     * 
     * @param serverId 服务器ID
     * @return 在线数量
     */
    int getOnlineCountByServer(int serverId);
}
