package com.muyi.core.bootstrap;

import com.muyi.core.module.ServerModule;

/**
 * 已启动的模块信息
 *
 * @author muyi
 */
class StartedModule {
    
    final String instanceId;
    final ServerModule module;
    
    StartedModule(String instanceId, ServerModule module) {
        this.instanceId = instanceId;
        this.module = module;
    }
}
