package com.muyi.world;

import com.muyi.core.module.AbstractGameModule;
import com.muyi.core.web.WebServer;
import com.muyi.rpc.server.RpcServer;
import com.muyi.shared.api.gate.IGateService;
import com.muyi.world.controller.WorldGmController;
import com.muyi.world.handler.WorldMessageDispatcher;
import com.muyi.world.player.WorldExecutorManager;
import com.muyi.world.service.WorldServiceImpl;

/**
 * World 模块骨架
 * 
 * 负责世界地图、活动、排行榜等全局数据管理，具体业务由各项目实现
 * 
 * 扩展方式：
 * 1. 继承此类，重写 doInit() 初始化业务组件
 * 2. 重写 registerRpcServices() 注册 RPC 服务
 * 3. 重写 registerWebRoutes() 注册 GM 接口
 * 4. 重写 registerMessageHandlers() 注册 World 消息处理器
 *
 * @author muyi
 */
public class WorldModule extends AbstractGameModule {

    private static final int DEFAULT_STRIPES = Runtime.getRuntime().availableProcessors();

    protected WorldGmController gmController;
    protected WorldExecutorManager executorManager;
    protected WorldMessageDispatcher messageDispatcher;
    private WorldServiceImpl worldService;

    @Override
    public String name() {
        return "world";
    }

    @Override
    public String description() {
        return "世界服务 - 地图/活动/排行榜管理";
    }

    @Override
    public int priority() {
        return 70;
    }

    @Override
    public int rpcPort() {
        return config != null ? config.getRpcPort() : 10004;
    }

    @Override
    public int webPort() {
        return config != null ? config.getWebPort() : 18004;
    }

    @Override
    protected void doInit() {
        // 初始化执行器管理器
        executorManager = new WorldExecutorManager(getPoolManager(), getStripeCount(), createGatePusher());

        // 初始化消息分发器
        messageDispatcher = new WorldMessageDispatcher(executorManager);
        registerMessageHandlers(messageDispatcher);

        // 初始化 RPC 服务
        worldService = new WorldServiceImpl(executorManager);
        worldService.setMessageDispatcher(messageDispatcher);

        // 初始化 GM 控制器
        gmController = createGmController();

        log.info("World module initialized, serverId={}, stripes={}",
                config != null ? config.getServerId() : -1, getStripeCount());
    }

    /**
     * 条带线程数，子类可重写
     */
    protected int getStripeCount() {
        return DEFAULT_STRIPES;
    }

    /**
     * 创建 gate 推送实现，子类可重写
     */
    protected com.muyi.world.player.WorldGatePusher createGatePusher() {
        return (gateServerId, playerId, protoId, message) -> {
            IGateService gate = getRpcProxy().get(IGateService.class, gateServerId);
            gate.pushMessage(playerId, protoId, message);
        };
    }

    /**
     * 注册 World 消息处理器
     * <p>
     * 默认自动扫描 {@link #getHandlerScanPackages()} 下所有 @MsgHandler 注解的 handler。
     * 子类可重写追加手动注册。
     */
    protected void registerMessageHandlers(WorldMessageDispatcher dispatcher) {
        dispatcher.scanAndRegister(getHandlerScanPackages(), this);
    }

    /**
     * Handler 扫描包名，子类可重写以添加更多包
     */
    protected String[] getHandlerScanPackages() {
        return new String[]{"com.muyi.world.handler"};
    }

    /**
     * 创建 GM 控制器，子类可重写以扩展
     */
    protected WorldGmController createGmController() {
        return new WorldGmController();
    }

    @Override
    protected void registerRpcServices(RpcServer server) {
        server.registerService(worldService);
    }

    @Override
    protected void registerWebRoutes(WebServer server) {
        server.registerController(gmController);
    }

    // ==================== Getter ====================

    public WorldExecutorManager getExecutorManager() {
        return executorManager;
    }

    public WorldMessageDispatcher getMessageDispatcher() {
        return messageDispatcher;
    }
}
