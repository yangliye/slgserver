package com.muyi.rpc.game;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.muyi.rpc.core.RpcFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.muyi.rpc.annotation.RpcService;
import com.muyi.rpc.gamei.DeductResult;
import com.muyi.rpc.gamei.IGameService;
import com.muyi.rpc.gamei.PlayerInfo;
import com.muyi.rpc.gamei.TransferResult;
import com.muyi.rpc.worldi.IWorldService;
import com.muyi.rpc.worldi.WorldEnterResult;

/**
 * Game 服务实现
 * 实现 IGameService 接口，支持 Game -> Game 的跨服 RPC 调用
 * 
 * @author muyi
 */
@RpcService(IGameService.class)
public class GameServiceImpl implements IGameService {
    
    private static final Logger logger = LoggerFactory.getLogger(GameServiceImpl.class);
    
    private final Map<Long, PlayerInfo> playerDatabase = new ConcurrentHashMap<>();
    
    private final GameServer gameServer;
    
    public GameServiceImpl(GameServer gameServer) {
        this.gameServer = gameServer;
    }
    
    // ==================== IGameService 接口实现（供其他服务器调用） ====================
    
    @Override
    public PlayerInfo getPlayerInfo(long playerId) {
        logger.info("[Game-{}] Getting player info for: {}", gameServer.getServerId(), playerId);
        return playerDatabase.get(playerId);
    }
    
    @Override
    public void sendMessageToPlayer(long playerId, String message) {
        logger.info("[Game-{}] Sending message to player {}: {}", gameServer.getServerId(), playerId, message);
        PlayerInfo player = playerDatabase.get(playerId);
        if (player != null && player.isOnline()) {
            logger.info("[Game-{}] Message delivered to player {}", gameServer.getServerId(), playerId);
        } else {
            logger.warn("[Game-{}] Player {} not online, message not delivered", gameServer.getServerId(), playerId);
        }
    }
    
    @Override
    public TransferResult transferGold(long fromPlayerId, long toPlayerId, long amount) {
        logger.info("[Game-{}] Transfer {} gold from {} to {}", 
                gameServer.getServerId(), amount, fromPlayerId, toPlayerId);
        
        PlayerInfo toPlayer = playerDatabase.get(toPlayerId);
        if (toPlayer == null) {
            return TransferResult.fail("Target player not found on server " + gameServer.getServerId());
        }
        
        toPlayer.setGold(toPlayer.getGold() + amount);
        return TransferResult.success(fromPlayerId, toPlayerId, amount, 0, toPlayer.getGold());
    }
    
    /**
     * 异步转账 - 服务端实现（ServerHandler 会自动转发到同步方法 transferGold）
     * 此方法实际不会被直接调用，但需要实现接口
     */
    @Override
    public RpcFuture transferGoldAsync(long fromPlayerId, long toPlayerId, long amount) {
        // 服务端直接返回同步结果，RpcFuture 是给客户端异步等待用的
        // ServerHandler 会自动调用 transferGold 方法
        throw new UnsupportedOperationException("Async method should not be called directly on server");
    }
    
    @Override
    public boolean isPlayerOnline(long playerId) {
        PlayerInfo player = playerDatabase.get(playerId);
        return player != null && player.isOnline();
    }
    
    @Override
    public DeductResult deductGold(long playerId, long amount) {
        logger.info("[Game-{}] Deducting {} gold from player {}", 
                gameServer.getServerId(), amount, playerId);
        
        PlayerInfo player = playerDatabase.get(playerId);
        if (player == null) {
            return DeductResult.fail("Player not found on server " + gameServer.getServerId());
        }
        
        if (player.getGold() < amount) {
            return DeductResult.fail("Insufficient gold. Current: " + player.getGold() + ", Required: " + amount);
        }
        
        // 扣除金币
        player.setGold(player.getGold() - amount);
        
        logger.info("[Game-{}] Player {} gold deducted: -{}, remaining: {}", 
                gameServer.getServerId(), playerId, amount, player.getGold());
        
        return DeductResult.success(playerId, amount, player.getGold());
    }
    
    @Override
    public RpcFuture deductGoldAsync(long playerId, long amount) {
        // 异步方法由 RpcProxy 自动处理，ServerHandler 会调用同步方法 deductGold
        throw new UnsupportedOperationException("Async method should not be called directly on server");
    }
    
    // ==================== 本地业务方法 ====================
    
    /**
     * 玩家登录
     */
    public PlayerInfo login(long playerId, String token) {
        logger.info("[Game-{}] Player {} login with token: {}", gameServer.getServerId(), playerId, token);
        
        if (token == null || token.isEmpty()) {
            logger.warn("[Game-{}] Invalid token for player {}", gameServer.getServerId(), playerId);
            return null;
        }
        
        PlayerInfo player = playerDatabase.computeIfAbsent(playerId, id -> {
            PlayerInfo newPlayer = new PlayerInfo();
            newPlayer.setPlayerId(id);
            newPlayer.setPlayerName("Player_" + id);
            newPlayer.setLevel(1);
            newPlayer.setGold(1000);
            newPlayer.setOnline(true);
            return newPlayer;
        });
        
        player.setOnline(true);
        logger.info("[Game-{}] Player {} logged in: {}", gameServer.getServerId(), playerId, player);
        return player;
    }
    
    /**
     * 登录并进入世界
     */
    public WorldEnterResult loginAndEnterWorld(long playerId, String token, int worldId) {
        logger.info("[Game-{}] Player {} login and enter world {}", gameServer.getServerId(), playerId, worldId);
        
        PlayerInfo player = login(playerId, token);
        if (player == null) {
            return WorldEnterResult.fail("Login failed");
        }
        
        IWorldService worldService = gameServer.getWorldService(worldId);
        if (worldService == null) {
            return WorldEnterResult.fail("WorldService-" + worldId + " not available");
        }
        
        try {
            logger.info("[Game-{}] Calling WorldService-{}.enterWorld via RPC...", gameServer.getServerId(), worldId);
            WorldEnterResult result = worldService.enterWorld(playerId, worldId);
            logger.info("[Game-{}] WorldService response: {}", gameServer.getServerId(), result.getMessage());
            return result;
        } catch (Exception e) {
            logger.error("[Game-{}] RPC call to WorldService failed", gameServer.getServerId(), e);
            return WorldEnterResult.fail("RPC call failed: " + e.getMessage());
        }
    }
    
    /**
     * 跨服转账（调用目标服务器的 Game 服务）
     */
    public TransferResult crossServerTransfer(long fromPlayerId, long toPlayerId, 
                                               long amount, int targetServerId) {
        logger.info("[Game-{}] Cross-server transfer {} gold from {} to {} on server {}", 
                gameServer.getServerId(), amount, fromPlayerId, toPlayerId, targetServerId);
        
        PlayerInfo fromPlayer = playerDatabase.get(fromPlayerId);
        if (fromPlayer == null) {
            return TransferResult.fail("Source player not found");
        }
        if (fromPlayer.getGold() < amount) {
            return TransferResult.fail("Insufficient gold");
        }
        
        IGameService targetGameService = gameServer.getGameService(targetServerId);
        if (targetGameService == null) {
            return TransferResult.fail("Target game server not available");
        }
        
        try {
            TransferResult result = targetGameService.transferGold(fromPlayerId, toPlayerId, amount);
            
            if (result.isSuccess()) {
                fromPlayer.setGold(fromPlayer.getGold() - amount);
                result.setFromPlayerBalance(fromPlayer.getGold());
            }
            
            return result;
        } catch (Exception e) {
            logger.error("[Game-{}] Cross-server transfer failed", gameServer.getServerId(), e);
            return TransferResult.fail("RPC call failed: " + e.getMessage());
        }
    }
}
