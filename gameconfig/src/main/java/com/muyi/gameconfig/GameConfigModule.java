package com.muyi.gameconfig;

import com.muyi.core.config.ModuleConfig;
import com.muyi.core.module.GameModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 游戏配置模块
 * 负责加载策划配置数据，可插拔部署
 * 
 * 优先级为 -1000，确保在所有业务模块之前启动
 * 
 * 配置项（通过 ModuleConfig.extra）：
 * - configRoot: 配置文件根目录，默认 "serverconfig/gamedata"
 * - configPackage: 配置类扫描包名，如 "com.muyi.game.config"
 *
 * @author muyi
 */
public class GameConfigModule implements GameModule {
    
    private static final Logger log = LoggerFactory.getLogger(GameConfigModule.class);
    
    private static final String DEFAULT_CONFIG_ROOT = "serverconfig/gamedata";
    
    private ModuleConfig moduleConfig;
    private String configRoot;
    private String configPackage;
    
    @Override
    public String name() {
        return "gameconfig";
    }
    
    @Override
    public int priority() {
        // 负数优先级，确保在所有业务模块之前启动
        return -1000;
    }
    
    @Override
    public void init(ModuleConfig config) {
        this.moduleConfig = config;
        
        // 从 extra 配置获取参数
        this.configRoot = config.getExtra("configRoot", DEFAULT_CONFIG_ROOT);
        this.configPackage = config.getExtra("configPackage", null);
        
        log.info("GameConfigModule initialized, configRoot={}, configPackage={}", 
                configRoot, configPackage);
    }
    
    @Override
    public void start() throws Exception {
        log.info("Loading game configs from: {}", configRoot);
        
        ConfigManager configManager = ConfigManager.getInstance();
        configManager.setConfigRoot(configRoot);
        
        // 自动扫描配置类
        if (configPackage != null && !configPackage.isEmpty()) {
            configManager.scan(configPackage);
        }
        
        // 加载所有配置
        configManager.loadAll();
        
        log.info("Game configs loaded, version={}, count={}", 
                configManager.getVersion(), configManager.getRegisteredCount());
    }
    
    @Override
    public void stop() {
        // 配置模块通常不需要清理
        log.info("GameConfigModule stopped");
    }
    
    @Override
    public boolean isRunning() {
        return ConfigManager.getInstance().getRegisteredCount() > 0;
    }
    
    @Override
    public int rpcPort() {
        // 配置模块不需要 RPC 服务
        return 0;
    }
    
    @Override
    public int webPort() {
        // 配置模块不需要 Web 服务
        return 0;
    }
}
