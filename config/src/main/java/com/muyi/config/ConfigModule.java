package com.muyi.config;

import com.muyi.core.config.ModuleConfig;
import com.muyi.core.module.GameModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ????
 * ????????????????
 * 
 * ???? -1000??????????????
 * 
 * ?????? ModuleConfig.extra??
 * - configRoot: ?????????? "serverconfig/gamedata"
 * - configPackage: ????????? "com.muyi.game.config"
 *
 * @author muyi
 */
public class ConfigModule implements GameModule {
    
    private static final Logger log = LoggerFactory.getLogger(ConfigModule.class);
    
    private static final String DEFAULT_CONFIG_ROOT = "serverconfig/gamedata";
    
    private ModuleConfig moduleConfig;
    private String configRoot;
    private String configPackage;
    
    @Override
    public String name() {
        return "config";
    }
    
    @Override
    public int priority() {
        // ???????????????????
        return -1000;
    }
    
    @Override
    public void init(ModuleConfig config) {
        this.moduleConfig = config;
        
        // ? extra ??????
        this.configRoot = config.getExtra("configRoot", DEFAULT_CONFIG_ROOT);
        this.configPackage = config.getExtra("configPackage", null);
        
        log.info("ConfigModule initialized, configRoot={}, configPackage={}", 
                configRoot, configPackage);
    }
    
    @Override
    public void start() throws Exception {
        log.info("Loading configs from: {}", configRoot);
        
        ConfigManager configManager = ConfigManager.getInstance();
        configManager.setConfigRoot(configRoot);
        
        // ???????
        if (configPackage != null && !configPackage.isEmpty()) {
            configManager.scan(configPackage);
        }
        
        // ??????
        configManager.loadAll();
        
        log.info("Configs loaded, version={}, count={}", 
                configManager.getVersion(), configManager.getRegisteredCount());
    }
    
    @Override
    public void stop() {
        log.info("ConfigModule stopped");
    }
    
    @Override
    public boolean isRunning() {
        return ConfigManager.getInstance().getRegisteredCount() > 0;
    }
    
    @Override
    public int rpcPort() {
        return 0;
    }
    
    @Override
    public int webPort() {
        return 0;
    }
}
