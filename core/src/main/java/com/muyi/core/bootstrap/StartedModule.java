package com.muyi.core.bootstrap;

import com.muyi.core.module.GameModule;

/**
 * 已启动的模块信息
 *
 * @author muyi
 */
class StartedModule {
    
    final String instanceId;
    final GameModule module;
    
    StartedModule(String instanceId, GameModule module) {
        this.instanceId = instanceId;
        this.module = module;
    }
}
