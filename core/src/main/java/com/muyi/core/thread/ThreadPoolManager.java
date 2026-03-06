package com.muyi.core.thread;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.muyi.core.module.AbstractGameModule;
import com.muyi.core.module.ModuleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 模块级线程池管理器
 * <p>
 * 统一创建、管理和监控模块内所有线程池。
 * 由模块线程创建，内部线程通过 ITL 自动继承模块上下文。
 * <p>
 * 内置监控：每 {@value #MONITOR_INTERVAL_SECONDS} 秒输出一次所有线程池状态。
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * ThreadPoolManager pools = getPoolManager();
 *
 * // 战斗计算线程池（4 线程）
 * ExecutorService battlePool = pools.newFixedPool("battle", 4);
 *
 * // IO 操作线程池（虚拟线程）
 * ExecutorService ioPool = pools.newVirtualPool("io");
 *
 * // 定时任务线程池
 * ScheduledExecutorService tickPool = pools.newScheduledPool("tick", 1);
 *
 * // 使用
 * battlePool.execute(() -> processBattle());
 *
 * // 获取已创建的线程池
 * ExecutorService pool = pools.get("battle");
 * }</pre>
 */
public class ThreadPoolManager {

    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolManager.class);

    private static final long MONITOR_INTERVAL_SECONDS = 180;

    private final String moduleName;
    private final AbstractGameModule owner;
    private final Map<String, ExecutorService> pools = new ConcurrentHashMap<>();
    private final ScheduledExecutorService monitor;
    private volatile boolean shutdown;

    public ThreadPoolManager(String moduleName, AbstractGameModule owner) {
        this.moduleName = moduleName;
        this.owner = owner;
        this.monitor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, moduleName + "-pool-monitor");
            t.setDaemon(true);
            return t;
        });
        this.monitor.scheduleAtFixedRate(this::logStats,
                MONITOR_INTERVAL_SECONDS, MONITOR_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    // ==================== 创建线程池 ====================

    /**
     * 创建固定大小线程池
     *
     * @param name    线程池名（模块内唯一）
     * @param threads 线程数
     * @return 线程池
     */
    public ExecutorService newFixedPool(String name, int threads) {
        checkName(name);
        ThreadPoolExecutor pool = new ModuleAwareThreadPool(
                threads, threads, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), platformFactory(name), owner);
        pools.put(name, pool);
        logger.info("Created fixed pool [{}] threads={}", name, threads);
        return pool;
    }

    /**
     * 创建可伸缩线程池
     *
     * @param name       线程池名
     * @param coreSize   核心线程数
     * @param maxSize    最大线程数
     * @param keepAliveSeconds 空闲线程存活秒数
     * @return 线程池
     */
    public ExecutorService newScalablePool(String name, int coreSize, int maxSize, long keepAliveSeconds) {
        checkName(name);
        ThreadPoolExecutor pool = new ModuleAwareThreadPool(
                coreSize, maxSize, keepAliveSeconds, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(), platformFactory(name), owner);
        pools.put(name, pool);
        logger.info("Created scalable pool [{}] core={}, max={}", name, coreSize, maxSize);
        return pool;
    }

    /**
     * 创建单线程线程池（保证任务顺序执行）
     *
     * @param name 线程池名
     * @return 线程池
     */
    public ExecutorService newSinglePool(String name) {
        return newFixedPool(name, 1);
    }

    /**
     * 创建定时线程池
     *
     * @param name    线程池名
     * @param threads 线程数
     * @return 定时线程池
     */
    public ScheduledExecutorService newScheduledPool(String name, int threads) {
        checkName(name);
        ScheduledThreadPoolExecutor pool = new ModuleAwareScheduledPool(
                threads, platformFactory(name), owner);
        pools.put(name, pool);
        logger.info("Created scheduled pool [{}] threads={}", name, threads);
        return pool;
    }

    /**
     * 创建虚拟线程池（适合 IO 密集型任务，无上限）
     *
     * @param name 线程池名
     * @return 虚拟线程池
     */
    public ExecutorService newVirtualPool(String name) {
        checkName(name);
        ThreadFactory vf = Thread.ofVirtual().name(moduleName + "-" + name + "-", 0).factory();
        AbstractGameModule mod = this.owner;
        ThreadFactory wrapped = r -> vf.newThread(() -> ModuleContext.runWith(mod, r));
        ExecutorService pool = Executors.newThreadPerTaskExecutor(wrapped);
        pools.put(name, pool);
        logger.info("Created virtual pool [{}]", name);
        return pool;
    }

    /**
     * 注册外部创建的线程池（纳入统一管理和监控）
     *
     * @param name     线程池名
     * @param executor 线程池实例
     */
    public void register(String name, ExecutorService executor) {
        checkName(name);
        pools.put(name, executor);
        logger.info("Registered external pool [{}]", name);
    }

    // ==================== 获取 ====================

    /**
     * 获取线程池
     *
     * @throws IllegalArgumentException 线程池不存在
     */
    @SuppressWarnings("unchecked")
    public <T extends ExecutorService> T get(String name) {
        ExecutorService pool = pools.get(name);
        if (pool == null) {
            throw new IllegalArgumentException("No pool named: " + name);
        }
        return (T) pool;
    }

    /**
     * 检查线程池是否存在
     */
    public boolean has(String name) {
        return pools.containsKey(name);
    }

    /**
     * 获取管理的线程池数量
     */
    public int getPoolCount() {
        return pools.size();
    }

    // ==================== 监控 ====================

    private void logStats() {
        if (pools.isEmpty() || shutdown) return;
        try {
            StringBuilder sb = new StringBuilder("Thread pools status:");
            for (var entry : pools.entrySet()) {
                sb.append("\n  ").append(entry.getKey()).append(": ");
                appendPoolStats(sb, entry.getValue());
            }
            logger.info("{}", sb);
        } catch (Exception e) {
            logger.error("Monitor error", e);
        }
    }

    private void appendPoolStats(StringBuilder sb, ExecutorService exec) {
        if (exec instanceof ThreadPoolExecutor tpe) {
            sb.append(String.format("threads=%d/%d, active=%d, queue=%d, completed=%d",
                    tpe.getPoolSize(), tpe.getMaximumPoolSize(),
                    tpe.getActiveCount(), tpe.getQueue().size(),
                    tpe.getCompletedTaskCount()));
            if (tpe.isShutdown()) {
                sb.append(" [SHUTDOWN]");
            }
        } else {
            sb.append("virtual");
            if (exec.isShutdown()) {
                sb.append(" [SHUTDOWN]");
            }
        }
    }

    // ==================== 生命周期 ====================

    /**
     * 关闭所有线程池（等待任务完成）
     */
    public void shutdown() {
        if (shutdown) return;
        shutdown = true;

        monitor.shutdown();

        for (var entry : pools.entrySet()) {
            String name = entry.getKey();
            ExecutorService pool = entry.getValue();
            pool.shutdown();
            try {
                if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                    logger.warn("Pool [{}] forced shutdown", name);
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        logger.info("ThreadPoolManager shutdown, {} pools closed", pools.size());
    }

    // ==================== Internal ====================

    private ThreadFactory platformFactory(String poolName) {
        return Thread.ofPlatform()
                .name(moduleName + "-" + poolName + "-", 0)
                .daemon(true)
                .factory();
    }

    private void checkName(String name) {
        if (shutdown) {
            throw new IllegalStateException("ThreadPoolManager is shutdown");
        }
        if (pools.containsKey(name)) {
            throw new IllegalArgumentException("Pool name already exists: " + name);
        }
    }
}
