package com.muyi.rpc.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.muyi.rpc.Rpc;
import com.muyi.rpc.client.RpcProxyManager;
import com.muyi.rpc.gamei.IGameService;
import com.muyi.rpc.gamei.PlayerInfo;
import com.muyi.rpc.gamei.TransferResult;
import com.muyi.rpc.server.RpcServer;
import com.muyi.rpc.worldi.IWorldService;
import com.muyi.rpc.worldi.WorldEnterResult;

/**
 * Game 服务器
 * 支持：
 * - Game -> World RPC 调用
 * - Game -> Game 跨服 RPC 调用
 *
 * @author muyi
 */
public class GameServer {
    
    private static final Logger logger = LoggerFactory.getLogger(GameServer.class);
    
    private final GameServerConfig config;
    
    private RpcServer rpcServer;
    private GameServiceImpl gameService;
    private RpcProxyManager rpc;
    
    public GameServer(GameServerConfig config) {
        this.config = config;
    }
    
    /**
     * 启动 Game 服务
     */
    public void start() throws Exception {
        logger.info("========== Starting Game Server [{}] ==========", config.getServerId());
        
        // 1. 初始化 RPC 客户端（用于调用其他服务）
        rpc = Rpc.connect(config.getZkAddress());
        
        // 2. 创建服务实现
        gameService = new GameServiceImpl(this);
        
        // 3. 使用 Builder 模式分散注册服务
        //    如果有多个服务，可以链式调用 register()
        rpcServer = Rpc.server(config.getPort())
                .serverId(config.getServerId())
                .zookeeper(config.getZkAddress())
                .register(gameService)       // 注册 GameService
                // .register(inventoryService) // 可以继续注册其他服务
                // .register(chatService)
                .start();
        
        logger.info("========== Game Server [{}] started ==========", config.getServerId());
    }
    
    /**
     * 关闭服务
     */
    public void shutdown() {
        logger.info("Shutting down Game Server [{}]...", config.getServerId());
        if (rpcServer != null) {
            rpcServer.shutdown();
        }
        if (rpc != null) {
            rpc.shutdown();
        }
        logger.info("Game Server [{}] stopped", config.getServerId());
    }
    
    // ==================== 获取其他服务代理 ====================
    
    /**
     * 获取指定 World 服务代理
     * 
     * @param serverId World 服务器ID
     */
    public IWorldService getWorldService(int serverId) {
        return rpc.get(IWorldService.class, serverId);
    }
    
    /**
     * 获取其他 Game 服务代理（跨服调用）
     * 
     * @param serverId 目标服务器ID
     */
    public IGameService getGameService(int serverId) {
        return rpc.get(IGameService.class, serverId);
    }
    
    // ==================== 对外服务接口 ====================
    
    public PlayerInfo login(long playerId, String token) {
        return gameService.login(playerId, token);
    }
    
    public PlayerInfo getPlayerInfo(long playerId) {
        return gameService.getPlayerInfo(playerId);
    }
    
    public WorldEnterResult loginAndEnterWorld(long playerId, String token, int worldId) {
        return gameService.loginAndEnterWorld(playerId, token, worldId);
    }
    
    public TransferResult crossServerTransfer(long fromPlayerId, long toPlayerId, 
                                               long amount, int targetServerId) {
        return gameService.crossServerTransfer(fromPlayerId, toPlayerId, amount, targetServerId);
    }
    
    public int getServerId() {
        return config.getServerId();
    }
    
    public int getPort() {
        return config.getPort();
    }
}
