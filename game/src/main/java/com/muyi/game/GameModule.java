package com.muyi.game;

import com.muyi.core.module.AbstractGameModule;
import com.muyi.core.web.WebServer;
import com.muyi.game.controller.GameGmController;
import com.muyi.game.handler.GameMessageDispatcher;
import com.muyi.game.player.GatePusher;
import com.muyi.game.player.PlayerExecutorManager;
import com.muyi.game.service.GameServiceImpl;
import com.muyi.rpc.server.RpcServer;
import com.muyi.shared.api.IGateService;

/**
 * Game 模块骨架
 * 
 * 负责玩家个人数据管理，具体业务由各项目实现
 * 
 * 扩展方式：
 * 1. 继承此类，重写 doInit() 初始化业务组件
 * 2. 重写 registerRpcServices() 注册 RPC 服务
 * 3. 重写 registerWebRoutes() 注册 GM 接口
 *
 * @author muyi
 */
public class GameModule extends AbstractGameModule {
    
    private static final int DEFAULT_PLAYER_STRIPES = Runtime.getRuntime().availableProcessors();
    
    protected GameGmController gmController;
    protected PlayerExecutorManager playerExecutorManager;
    protected GameMessageDispatcher messageDispatcher;
    private GameServiceImpl gameService;
    
    @Override
    public String name() {
        return "game";
    }
    
    @Override
    public String description() {
        return "游戏核心服务 - 玩家数据管理";
    }
    
    @Override
    public int priority() {
        return 80;
    }
    
    @Override
    public int rpcPort() {
        return config != null ? config.getRpcPort() : 10003;
    }
    
    @Override
    public int webPort() {
        return config != null ? config.getWebPort() : 18003;
    }
    
    @Override
    protected void doInit() {
        com.muyi.proto.MessageRegistry.init();
        
        // 初始化玩家执行器管理器
        GatePusher gatePusher = createGatePusher();
        playerExecutorManager = new PlayerExecutorManager(
                getPoolManager(), getPlayerStripes(), gatePusher);
        
        // 初始化消息分发器
        messageDispatcher = new GameMessageDispatcher(playerExecutorManager);
        registerMessageHandlers(messageDispatcher);
        
        // 初始化 RPC 服务
        gameService = new GameServiceImpl(playerExecutorManager);
        gameService.setMessageDispatcher(messageDispatcher);
        
        // 初始化 GM 控制器
        gmController = createGmController();
        
        log.info("Game module initialized, serverId={}, playerStripes={}", 
                config != null ? config.getServerId() : -1, getPlayerStripes());
    }
    
    /**
     * 创建 gate 推送实现，子类可重写
     */
    protected GatePusher createGatePusher() {
        return (gateServerId, playerId, protoId, message) -> {
            IGateService gate = getRpcProxy().get(IGateService.class, gateServerId);
            gate.pushMessage(playerId, protoId, message);
        };
    }
    
    /**
     * 玩家条带线程数，子类可重写
     */
    protected int getPlayerStripes() {
        return DEFAULT_PLAYER_STRIPES;
    }
    
    /**
     * 注册消息处理器，子类重写以添加更多业务 handler
     */
    protected void registerMessageHandlers(GameMessageDispatcher dispatcher) {
        dispatcher.register(com.muyi.proto.MsgId.PLAYER_LOGIN_REQ_VALUE,
                new com.muyi.game.handler.player.PlayerLoginHandler(playerExecutorManager));
    }
    
    /**
     * 创建 GM 控制器，子类可重写以扩展
     */
    protected GameGmController createGmController() {
        return new GameGmController();
    }
    
    @Override
    protected void registerRpcServices(RpcServer server) {
        server.registerService(gameService);
    }
    
    @Override
    protected void registerWebRoutes(WebServer server) {
        server.registerController(gmController);
    }
    
    // ==================== Getter ====================
    
    public PlayerExecutorManager getPlayerExecutorManager() {
        return playerExecutorManager;
    }
    
    public GameMessageDispatcher getMessageDispatcher() {
        return messageDispatcher;
    }
}
