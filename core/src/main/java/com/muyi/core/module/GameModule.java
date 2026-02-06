package com.muyi.core.module;

import com.muyi.core.config.ModuleConfig;

/**
 * 游戏模块接口
 * 所有业务模块（login、gate、game、world、alliance）都需要实现此接口
 *
 * @author muyi
 */
public interface GameModule {
    
    /**
     * 模块名称（唯一标识）
     * 如：login, gate, game, world, alliance
     */
    String name();
    
    /**
     * 模块描述
     */
    default String description() {
        return name() + " module";
    }
    
    /**
     * 初始化模块
     * 在此阶段加载配置、初始化资源，但不启动服务
     *
     * @param config 模块配置
     */
    void init(ModuleConfig config);
    
    /**
     * 启动模块
     * 启动 RPC 服务、Web 服务等
     *
     * @throws Exception 启动异常
     */
    void start() throws Exception;
    
    /**
     * 停止模块
     * 优雅关闭，释放资源
     */
    void stop();
    
    /**
     * 是否已启动
     */
    boolean isRunning();
    
    /**
     * RPC 服务端口
     * 返回 0 表示不启用 RPC 服务
     */
    int rpcPort();
    
    /**
     * Web 服务端口（GM 后台）
     * 返回 0 表示不启用 Web 服务
     */
    int webPort();
    
    /**
     * 模块优先级（启动顺序）
     * 数值越小越先启动
     * 默认：login=10, gate=20, game=30, world=40, alliance=50
     */
    default int priority() {
        return 100;
    }
}
