package com.muyi.rpc.transport;

import io.netty.channel.EventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 共享 EventLoopGroup
 * 多个 RpcServer 共享同一组 boss/worker 线程组，减少线程资源消耗
 *
 * <p>使用引用计数管理生命周期：
 * <ul>
 *   <li>{@link #acquire()} 获取引用（引用计数+1）</li>
 *   <li>{@link #release()} 释放引用（引用计数-1，归零时关闭线程组）</li>
 * </ul>
 *
 * @author muyi
 */
public class SharedEventLoopGroup {

    private static final Logger logger = LoggerFactory.getLogger(SharedEventLoopGroup.class);

    private static volatile SharedEventLoopGroup instance;

    private final TransportType transport;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final AtomicInteger refCount = new AtomicInteger(0);
    private static volatile int shutdownTimeoutSeconds = 15;

    private SharedEventLoopGroup(int workerThreads) {
        this.transport = TransportType.detect();
        this.bossGroup = transport.createEventLoopGroup(1);
        this.workerGroup = transport.createEventLoopGroup(workerThreads);
        logger.info("SharedEventLoopGroup created, transport={}, workerThreads={}",
                transport.name(), workerThreads <= 0 ? "default" : workerThreads);
    }

    /**
     * 初始化共享实例（进程启动时调用一次）
     *
     * @param workerThreads worker 线程数，0 表示使用默认值（CPU 核心数）
     */
    /**
     * 设置关闭等待时间（在 init 之前调用）
     */
    public static void setShutdownTimeoutSeconds(int seconds) {
        shutdownTimeoutSeconds = seconds;
    }

    public static synchronized void init(int workerThreads) {
        if (instance != null) {
            logger.warn("SharedEventLoopGroup already initialized, skip");
            return;
        }
        instance = new SharedEventLoopGroup(workerThreads);
    }

    /**
     * 获取共享实例（必须先调用 init）
     *
     * @throws IllegalStateException 未初始化时抛出
     */
    public static SharedEventLoopGroup getInstance() {
        SharedEventLoopGroup local = instance;
        if (local == null) {
            throw new IllegalStateException(
                    "SharedEventLoopGroup not initialized. Call SharedEventLoopGroup.init() in Bootstrap before starting modules.");
        }
        return local;
    }

    /**
     * 获取引用（引用计数+1）
     * 每个 RpcServer 启动时调用
     */
    public SharedEventLoopGroup acquire() {
        int count = refCount.incrementAndGet();
        logger.debug("SharedEventLoopGroup acquired, refCount={}", count);
        return this;
    }

    /**
     * 释放引用（引用计数-1）
     * 每个 RpcServer 关闭时调用，归零时关闭线程组
     */
    public void release() {
        int count = refCount.decrementAndGet();
        logger.debug("SharedEventLoopGroup released, refCount={}", count);
        if (count <= 0) {
            shutdown();
        }
    }

    /**
     * 强制关闭（进程退出时调用）
     */
    public static synchronized void shutdownGlobal() {
        if (instance != null) {
            instance.shutdown();
            instance = null;
        }
    }

    private void shutdown() {
        logger.info("SharedEventLoopGroup shutting down...");
        try {
            bossGroup.shutdownGracefully().await(shutdownTimeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            workerGroup.shutdownGracefully().await(shutdownTimeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        logger.info("SharedEventLoopGroup shutdown complete");
    }

    public EventLoopGroup bossGroup() {
        return bossGroup;
    }

    public EventLoopGroup workerGroup() {
        return workerGroup;
    }

    public TransportType transport() {
        return transport;
    }

    public int refCount() {
        return refCount.get();
    }
}
