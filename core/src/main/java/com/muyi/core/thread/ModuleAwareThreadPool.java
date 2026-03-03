package com.muyi.core.thread;

import com.muyi.core.module.AbstractGameModule;
import com.muyi.core.module.ModuleContext;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 模块感知的线程池，每次任务执行前自动设置 {@link ModuleContext} 上下文。
 */
class ModuleAwareThreadPool extends ThreadPoolExecutor {

    private final AbstractGameModule owner;

    ModuleAwareThreadPool(int corePoolSize, int maximumPoolSize,
                          long keepAliveTime, TimeUnit unit,
                          BlockingQueue<Runnable> workQueue,
                          ThreadFactory threadFactory,
                          AbstractGameModule owner) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        this.owner = owner;
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        ModuleContext.setCurrent(owner);
        super.beforeExecute(t, r);
    }
}
