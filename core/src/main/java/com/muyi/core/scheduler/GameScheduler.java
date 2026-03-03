package com.muyi.core.scheduler;

import java.time.DayOfWeek;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.muyi.common.util.time.TimeUtils;

/**
 * 游戏定时任务调度器（每模块独立实例）
 * <p>
 * 纯调度逻辑层，底层线程池由 {@link com.muyi.core.thread.ThreadPoolManager} 创建和监控。
 * 由模块线程创建，内部线程通过 ITL 自动继承模块上下文，业务方无需关心日志归属。
 * <p>
 * 支持调度模式：
 * <ul>
 *   <li>固定频率 / 固定延迟</li>
 *   <li>每天 HH:mm</li>
 *   <li>每周 DAY HH:mm</li>
 *   <li>每小时 :mm</li>
 *   <li>每分钟 :ss</li>
 * </ul>
 *
 * <h3>使用示例（在模块 doInit 中）：</h3>
 * <pre>{@code
 * initScheduler(2);
 * getScheduler().scheduleDaily("daily-reset", 5, 0, this::resetDaily);
 * getScheduler().scheduleFixedRate("resource-tick", 30, TimeUnit.SECONDS, this::produceResource);
 * }</pre>
 */
public class GameScheduler {

    private static final Logger logger = LoggerFactory.getLogger(GameScheduler.class);

    private final ScheduledExecutorService executor;
    private final CopyOnWriteArrayList<ScheduledGameTask> tasks = new CopyOnWriteArrayList<>();
    private volatile boolean shutdown;

    /**
     * 创建调度器
     *
     * @param executor 底层线程池（由 ThreadPoolManager 创建和管理）
     */
    public GameScheduler(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    // ==================== Fixed Rate / Delay ====================

    /**
     * 固定频率调度（立即首次执行，之后每隔 period 执行）
     *
     * @param name   任务名（调度器内唯一）
     * @param period 间隔
     * @param unit   时间单位
     * @param task   执行体
     */
    public ScheduledGameTask scheduleFixedRate(String name, long period, TimeUnit unit, Runnable task) {
        checkDuplicate(name);
        ScheduledGameTask t = new ScheduledGameTask(name);
        t.setFuture(executor.scheduleAtFixedRate(
                () -> safeRun(name, task), 0, period, unit));
        tasks.add(t);
        logger.info("Scheduled fixed-rate [{}] period={}{}", name, period, abbr(unit));
        return t;
    }

    /**
     * 固定频率调度（延迟首次执行）
     */
    public ScheduledGameTask scheduleFixedRate(String name, long initialDelay, long period,
                                               TimeUnit unit, Runnable task) {
        checkDuplicate(name);
        ScheduledGameTask t = new ScheduledGameTask(name);
        t.setFuture(executor.scheduleAtFixedRate(
                () -> safeRun(name, task), initialDelay, period, unit));
        tasks.add(t);
        logger.info("Scheduled fixed-rate [{}] delay={}{} period={}{}", name,
                initialDelay, abbr(unit), period, abbr(unit));
        return t;
    }

    /**
     * 固定延迟调度（上次执行完成后，等待 delay 再执行下一次）
     */
    public ScheduledGameTask scheduleFixedDelay(String name, long delay, TimeUnit unit, Runnable task) {
        checkDuplicate(name);
        ScheduledGameTask t = new ScheduledGameTask(name);
        t.setFuture(executor.scheduleWithFixedDelay(
                () -> safeRun(name, task), delay, delay, unit));
        tasks.add(t);
        logger.info("Scheduled fixed-delay [{}] delay={}{}", name, delay, abbr(unit));
        return t;
    }

    // ==================== Time Point ====================

    /**
     * 每天定时执行
     *
     * @param name   任务名
     * @param hour   小时（0-23）
     * @param minute 分钟（0-59）
     * @param task   执行体
     */
    public ScheduledGameTask scheduleDaily(String name, int hour, int minute, Runnable task) {
        checkDuplicate(name);
        ScheduledGameTask t = new ScheduledGameTask(name);
        LongSupplier delayMs = () -> TimeUtils.nextDailyMillis(TimeUtils.currentTimeMillis(), hour, minute)
                - TimeUtils.currentTimeMillis();
        scheduleTimePoint(t, task, delayMs);
        tasks.add(t);
        logger.info("Scheduled daily [{}] at {}:{}", name, fmt(hour), fmt(minute));
        return t;
    }

    /**
     * 每周定时执行
     *
     * @param name      任务名
     * @param dayOfWeek 星期几
     * @param hour      小时（0-23）
     * @param minute    分钟（0-59）
     * @param task      执行体
     */
    public ScheduledGameTask scheduleWeekly(String name, DayOfWeek dayOfWeek, int hour, int minute,
                                            Runnable task) {
        checkDuplicate(name);
        ScheduledGameTask t = new ScheduledGameTask(name);
        LongSupplier delayMs = () -> TimeUtils.nextWeeklyMillis(
                TimeUtils.currentTimeMillis(), dayOfWeek, hour, minute) - TimeUtils.currentTimeMillis();
        scheduleTimePoint(t, task, delayMs);
        tasks.add(t);
        logger.info("Scheduled weekly [{}] {} {}:{}", name, dayOfWeek, fmt(hour), fmt(minute));
        return t;
    }

    /**
     * 每小时定时执行
     *
     * @param name   任务名
     * @param minute 分钟（0-59），每小时的第几分钟执行
     * @param task   执行体
     */
    public ScheduledGameTask scheduleHourly(String name, int minute, Runnable task) {
        checkDuplicate(name);
        ScheduledGameTask t = new ScheduledGameTask(name);
        LongSupplier delayMs = () -> TimeUtils.nextHourlyMillis(TimeUtils.currentTimeMillis(), minute)
                - TimeUtils.currentTimeMillis();
        scheduleTimePoint(t, task, delayMs);
        tasks.add(t);
        logger.info("Scheduled hourly [{}] at :{}", name, fmt(minute));
        return t;
    }

    /**
     * 每分钟定时执行
     *
     * @param name   任务名
     * @param second 秒（0-59），每分钟的第几秒执行
     * @param task   执行体
     */
    public ScheduledGameTask schedulePerMinute(String name, int second, Runnable task) {
        checkDuplicate(name);
        ScheduledGameTask t = new ScheduledGameTask(name);
        LongSupplier delayMs = () -> TimeUtils.nextMinuteMillis(TimeUtils.currentTimeMillis(), second)
                - TimeUtils.currentTimeMillis();
        scheduleTimePoint(t, task, delayMs);
        tasks.add(t);
        logger.info("Scheduled per-minute [{}] at ::{}", name, fmt(second));
        return t;
    }

    // ==================== Convenience ====================

    /**
     * 每秒执行
     */
    public ScheduledGameTask scheduleEverySecond(String name, Runnable task) {
        return scheduleFixedRate(name, 1, TimeUnit.SECONDS, task);
    }

    // ==================== Cancel ====================

    /**
     * 按名称取消任务
     */
    public boolean cancel(String name) {
        for (ScheduledGameTask task : tasks) {
            if (task.getName().equals(name) && !task.isCancelled()) {
                task.cancel();
                logger.info("Cancelled task [{}]", name);
                return true;
            }
        }
        return false;
    }

    /**
     * 取消所有任务
     */
    public void cancelAll() {
        int count = 0;
        for (ScheduledGameTask task : tasks) {
            if (!task.isCancelled()) {
                task.cancel();
                count++;
            }
        }
        if (count > 0) {
            logger.info("Cancelled all {} tasks", count);
        }
    }

    // ==================== Lifecycle ====================

    /**
     * 关闭调度器（只取消任务，不关闭线程池——线程池由 ThreadPoolManager 管理）
     */
    public void shutdown() {
        if (shutdown) return;
        shutdown = true;
        cancelAll();
        logger.info("GameScheduler shutdown");
    }

    /**
     * 获取活跃任务数
     */
    public int getActiveTaskCount() {
        int count = 0;
        for (ScheduledGameTask task : tasks) {
            if (!task.isCancelled()) count++;
        }
        return count;
    }

    // ==================== Internal ====================

    private void scheduleTimePoint(ScheduledGameTask t, Runnable task, LongSupplier delayMsSupplier) {
        if (t.isCancelled() || shutdown) return;
        long delayMs = Math.max(0, delayMsSupplier.getAsLong());
        t.setFuture(executor.schedule(() -> {
            if (t.isCancelled()) return;
            safeRun(t.getName(), task);
            scheduleTimePoint(t, task, delayMsSupplier);
        }, delayMs, TimeUnit.MILLISECONDS));
    }

    private void safeRun(String name, Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            logger.error("Task [{}] error", name, e);
        }
    }

    private void checkDuplicate(String name) {
        for (ScheduledGameTask existing : tasks) {
            if (!existing.isCancelled() && existing.getName().equals(name)) {
                throw new IllegalArgumentException("Duplicate task name: " + name);
            }
        }
    }

    private static String fmt(int value) {
        return String.format("%02d", value);
    }

    private static String abbr(TimeUnit unit) {
        return switch (unit) {
            case NANOSECONDS -> "ns";
            case MICROSECONDS -> "us";
            case MILLISECONDS -> "ms";
            case SECONDS -> "s";
            case MINUTES -> "min";
            case HOURS -> "h";
            case DAYS -> "d";
        };
    }
}
