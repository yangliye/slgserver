package com.muyi.core.thread;

import com.muyi.core.module.AbstractGameModule;
import com.muyi.core.module.ModuleContext;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * 模块感知的定时线程池，每次任务执行时自动绑定 {@link ModuleContext} 上下文。
 * <p>
 * 覆写所有任务提交入口，确保 ScopedValue 在任务执行时可用。
 * {@code execute()} 内部调用 {@code schedule()}，无需单独覆写。
 */
class ModuleAwareScheduledPool extends ScheduledThreadPoolExecutor {

    private final AbstractGameModule owner;

    ModuleAwareScheduledPool(int corePoolSize, ThreadFactory threadFactory,
                             AbstractGameModule owner) {
        super(corePoolSize, threadFactory);
        this.owner = owner;
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return super.schedule(() -> ModuleContext.runWith(owner, command), delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return super.schedule(() -> ModuleContext.callWith(owner, callable), delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay,
                                                   long period, TimeUnit unit) {
        return super.scheduleAtFixedRate(() -> ModuleContext.runWith(owner, command),
                initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay,
                                                      long delay, TimeUnit unit) {
        return super.scheduleWithFixedDelay(() -> ModuleContext.runWith(owner, command),
                initialDelay, delay, unit);
    }
}
