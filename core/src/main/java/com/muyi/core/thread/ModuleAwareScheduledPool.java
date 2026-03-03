package com.muyi.core.thread;

import com.muyi.core.module.AbstractGameModule;
import com.muyi.core.module.ModuleContext;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

/**
 * 模块感知的定时线程池，每次任务执行前自动设置 {@link ModuleContext} 上下文。
 */
class ModuleAwareScheduledPool extends ScheduledThreadPoolExecutor {

    private final AbstractGameModule owner;

    ModuleAwareScheduledPool(int corePoolSize, ThreadFactory threadFactory,
                             AbstractGameModule owner) {
        super(corePoolSize, threadFactory);
        this.owner = owner;
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        ModuleContext.setCurrent(owner);
        super.beforeExecute(t, r);
    }
}
