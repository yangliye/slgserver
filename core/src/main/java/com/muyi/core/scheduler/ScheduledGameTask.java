package com.muyi.core.scheduler;

import java.util.concurrent.ScheduledFuture;

/**
 * 定时任务封装
 * <p>
 * 通过 {@link GameScheduler} 的 schedule 方法注册，不可直接构造。
 * 持有底层 {@link ScheduledFuture} 引用，支持取消操作。
 */
public class ScheduledGameTask {

    private final String name;
    private volatile ScheduledFuture<?> future;
    private volatile boolean cancelled;

    ScheduledGameTask(String name) {
        this.name = name;
    }

    void setFuture(ScheduledFuture<?> future) {
        this.future = future;
    }

    public String getName() {
        return name;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        cancelled = true;
        ScheduledFuture<?> f = future;
        if (f != null) {
            f.cancel(false);
        }
    }
}
