package com.muyi.db.async;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.muyi.common.util.time.TimeUtils;

/**
 * 工作线程：每个线程有独立的队列，负责处理分配给它的任务
 * <p>
 * 同一个表的数据固定分配到同一个线程，保证顺序且无需锁
 */
public class WorkerThread extends Thread {
    
    private static final Logger logger = LoggerFactory.getLogger(WorkerThread.class);
    
    private final int workerId;
    
    /**
     * 阻塞队列（内置 wait/notify 机制）
     */
    private final BlockingQueue<LandTask> queue = new LinkedBlockingQueue<>();
    
    /**
     * 批量处理器
     */
    private final Consumer<List<LandTask>> batchProcessor;
    
    /**
     * 批量大小（支持动态调整）
     */
    private volatile int batchSize;
    
    /**
     * 落地间隔（毫秒，支持动态调整）
     */
    private volatile long landIntervalMs;
    
    /**
     * 基础批量大小（自适应调整的基准值）
     */
    private final int baseBatchSize;
    
    /**
     * 基础落地间隔（自适应调整的基准值）
     */
    private final long baseLandIntervalMs;
    
    /**
     * 运行标志
     */
    private volatile boolean running = true;
    
    /**
     * 是否启用自适应调整
     */
    private volatile boolean adaptiveEnabled = true;
    
    /**
     * 队列积压阈值（超过此值认为积压严重）
     */
    private volatile int backlogThreshold = 1000;
    
    /**
     * 队列空闲阈值（低于此值认为空闲）
     */
    private volatile int idleThreshold = 100;
    
    /**
     * 上次调整时间（避免频繁调整）
     */
    private volatile long lastAdjustTime = 0;
    
    /**
     * 调整间隔（毫秒，避免频繁调整）
     */
    private static final long ADJUST_INTERVAL_MS = 5000; // 5秒
    
    /**
     * 自适应调整因子
     */
    private static final double BACKLOG_INTERVAL_FACTOR = 0.5;   // 积压时间隔缩短到 50%
    private static final double BACKLOG_BATCH_FACTOR = 2.0;      // 积压时批量增大到 200%
    private static final double IDLE_INTERVAL_FACTOR = 2.0;      // 空闲时间隔延长到 200%
    private static final double IDLE_BATCH_FACTOR = 0.5;         // 空闲时批量缩小到 50%
    
    /**
     * 滞后因子：防止在阈值边界频繁切换
     */
    private static final double HYSTERESIS_FACTOR = 0.8;
    
    /**
     * 自适应状态枚举
     */
    private enum AdaptiveState { NORMAL, BACKLOG, IDLE }
    
    /**
     * 当前自适应状态
     */
    private AdaptiveState currentState = AdaptiveState.NORMAL;
    
    public WorkerThread(int workerId, long landIntervalMs, int batchSize,
                        Consumer<List<LandTask>> batchProcessor) {
        super("muyi-db-worker-" + workerId);
        this.workerId = workerId;
        this.landIntervalMs = landIntervalMs;
        this.batchSize = batchSize;
        this.baseLandIntervalMs = landIntervalMs;
        this.baseBatchSize = batchSize;
        this.batchProcessor = batchProcessor;
        setDaemon(true);
    }
    
    /**
     * 获取工作线程 ID
     */
    public int getWorkerId() {
        return workerId;
    }
    
    /**
     * 提交任务
     */
    public void submit(LandTask task) {
        try {
            queue.put(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 停止工作线程
     */
    public void shutdownWorker() {
        running = false;
        // 放入毒丸，确保 poll() 能退出阻塞
        try {
            queue.put(LandTask.POISON_PILL);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 检查队列是否为空
     */
    public boolean isQueueEmpty() {
        return queue.isEmpty();
    }
    
    /**
     * 获取队列中的任务数
     */
    public int getQueueSize() {
        return queue.size();
    }
    
    /**
     * 动态调整批量大小
     * 
     * @param batchSize 新的批量大小（至少为 1）
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = Math.max(1, batchSize);
    }
    
    /**
     * 动态调整落地间隔
     * 
     * @param landIntervalMs 新的落地间隔（至少为 1ms）
     */
    public void setLandIntervalMs(long landIntervalMs) {
        this.landIntervalMs = Math.max(1, landIntervalMs);
    }
    
    /**
     * 启用/禁用自适应调整
     */
    public void setAdaptiveEnabled(boolean enabled) {
        this.adaptiveEnabled = enabled;
        if (!enabled) {
            // 禁用时恢复基础值和状态
            this.batchSize = baseBatchSize;
            this.landIntervalMs = baseLandIntervalMs;
            this.currentState = AdaptiveState.NORMAL;
        }
    }
    
    /**
     * 设置队列积压阈值
     */
    public void setBacklogThreshold(int threshold) {
        this.backlogThreshold = threshold;
    }
    
    /**
     * 设置队列空闲阈值
     */
    public void setIdleThreshold(int threshold) {
        this.idleThreshold = threshold;
    }
    
    @Override
    public void run() {
        logger.debug("Worker-{} started", workerId);
        
        boolean receivedPoisonPill = false;
        
        while ((running || !queue.isEmpty()) && !receivedPoisonPill) {
            try {
                List<LandTask> batch = new ArrayList<>(batchSize);
                receivedPoisonPill = collectBatch(batch);
                
                // 批量处理
                if (!batch.isEmpty()) {
                    batchProcessor.accept(batch);
                }
                
                // 自适应调整参数
                if (adaptiveEnabled) {
                    adaptiveAdjust();
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Worker-{} error", workerId, e);
            }
        }
        
        // 关闭前处理剩余任务
        List<LandTask> remaining = new ArrayList<>();
        queue.drainTo(remaining);
        remaining.removeIf(LandTask::isPoisonPill);
        if (!remaining.isEmpty()) {
            batchProcessor.accept(remaining);
        }
        
        logger.debug("Worker-{} stopped", workerId);
    }
    
    /**
     * 收集一批任务（定量优先 + 超时兜底）
     * <p>
     * 优化策略：
     * <ol>
     *   <li>先用 drainTo 非阻塞批量取（高流量时立即返回）</li>
     *   <li>如果不够，再阻塞等待剩余时间（低流量时等待）</li>
     * </ol>
     * <p>
     * 优势：
     * <ul>
     *   <li>高流量：drainTo 立即取够，不阻塞，性能最优</li>
     *   <li>低流量：先取一部分，再阻塞等待，避免空等</li>
     * </ul>
     *
     * @param batch 用于存放收集到的任务
     * @return 是否收到毒丸（收到毒丸表示应该退出）
     */
    private boolean collectBatch(List<LandTask> batch) throws InterruptedException {
        long deadline = TimeUtils.currentTimeMillis() + landIntervalMs;
        
        // 步骤1：先用 drainTo 非阻塞批量取（高流量时立即取够）
        int needed = batchSize - batch.size();
        if (needed > 0 && !queue.isEmpty()) {
            queue.drainTo(batch, needed);
            // 过滤掉毒丸（如果 drainTo 取到了毒丸）
            if (batch.removeIf(LandTask::isPoisonPill)) {
                return true;  // 收到毒丸
            }
        }
        
        // 步骤2：如果已经取够，直接返回（避免阻塞）
        if (batch.size() >= batchSize) {
            return false;
        }
        
        // 步骤3：不够的话，阻塞等待剩余时间（低流量时等待新任务）
        while (batch.size() < batchSize) {
            long remaining = deadline - TimeUtils.currentTimeMillis();
            if (remaining <= 0) {
                break;  // 超时兜底
            }
            
            LandTask task = queue.poll(remaining, TimeUnit.MILLISECONDS);
            if (task == null) {
                break;  // 超时，返回已收集的任务
            }
            if (task.isPoisonPill()) {
                return true;  // 收到毒丸，通知调用者退出
            }
            batch.add(task);
        }
        
        return false;
    }
    
    /**
     * 自适应调整参数（根据队列长度）
     * <p>
     * 策略：
     * <ul>
     *   <li>队列积压严重（>backlogThreshold）：缩短间隔、增大批量，加快处理</li>
     *   <li>队列空闲（<idleThreshold）：延长间隔、减小批量，充分合并</li>
     *   <li>正常范围：恢复基础值</li>
     * </ul>
     * <p>
     * 使用滞后机制防止在阈值边界频繁切换
     */
    private void adaptiveAdjust() {
        long now = TimeUtils.currentTimeMillis();
        if (now - lastAdjustTime < ADJUST_INTERVAL_MS) {
            return;  // 避免频繁调整
        }
        lastAdjustTime = now;
        
        int queueSize = queue.size();
        int oldBatchSize = batchSize;
        long oldInterval = landIntervalMs;
        AdaptiveState newState = currentState;
        
        // 使用滞后机制判断状态切换
        switch (currentState) {
            case NORMAL:
                if (queueSize > backlogThreshold) {
                    newState = AdaptiveState.BACKLOG;
                } else if (queueSize < idleThreshold) {
                    newState = AdaptiveState.IDLE;
                }
                break;
            case BACKLOG:
                // 从积压状态恢复需要降到阈值的 80%
                if (queueSize < backlogThreshold * HYSTERESIS_FACTOR) {
                    if (queueSize < idleThreshold) {
                        newState = AdaptiveState.IDLE;
                    } else {
                        newState = AdaptiveState.NORMAL;
                    }
                }
                break;
            case IDLE:
                // 从空闲状态恢复需要超过阈值的 120%
                if (queueSize > idleThreshold / HYSTERESIS_FACTOR) {
                    if (queueSize > backlogThreshold) {
                        newState = AdaptiveState.BACKLOG;
                    } else {
                        newState = AdaptiveState.NORMAL;
                    }
                }
                break;
        }
        
        // 状态没变，无需调整
        if (newState == currentState) {
            return;
        }
        
        currentState = newState;
        
        // 根据新状态设置参数
        switch (newState) {
            case BACKLOG:
                landIntervalMs = Math.max(1, (long) (baseLandIntervalMs * BACKLOG_INTERVAL_FACTOR));
                batchSize = Math.max(1, (int) (baseBatchSize * BACKLOG_BATCH_FACTOR));
                logger.info("Worker-{} adaptive BACKLOG: queue={}, interval={}ms->{}ms, batch={}->{}", 
                        workerId, queueSize, oldInterval, landIntervalMs, oldBatchSize, batchSize);
                break;
            case IDLE:
                landIntervalMs = Math.max(1, (long) (baseLandIntervalMs * IDLE_INTERVAL_FACTOR));
                batchSize = Math.max(1, (int) (baseBatchSize * IDLE_BATCH_FACTOR));
                logger.info("Worker-{} adaptive IDLE: queue={}, interval={}ms->{}ms, batch={}->{}", 
                        workerId, queueSize, oldInterval, landIntervalMs, oldBatchSize, batchSize);
                break;
            case NORMAL:
                batchSize = baseBatchSize;
                landIntervalMs = baseLandIntervalMs;
                logger.info("Worker-{} adaptive NORMAL: queue={}, interval={}ms->{}ms, batch={}->{}", 
                        workerId, queueSize, oldInterval, landIntervalMs, oldBatchSize, batchSize);
                break;
        }
    }
}
