package com.muyi.db.config;

/**
 * 数据库落地模式配置
 * <p>
 * 定义一组落地参数，可用于 {@link SLGDbConfigManager} 的模式切换，
 * 也可独立使用或从配置文件加载。
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 方式1：构造函数
 * ModeConfig config = new ModeConfig(500, 20, true, 2000, 200);
 * 
 * // 方式2：Builder 风格
 * ModeConfig config = new ModeConfig()
 *     .setBatchSize(500)
 *     .setIntervalMs(20)
 *     .setAdaptiveEnabled(true);
 * 
 * // 应用配置
 * configManager.applyCustomConfig(config);
 * }</pre>
 */
public class ModeConfig {
    
    /** 批量大小 */
    private int batchSize = 200;
    
    /** 落地间隔（毫秒） */
    private long intervalMs = 50;
    
    /** 是否启用自适应调整 */
    private boolean adaptiveEnabled = true;
    
    /** 队列积压阈值（自适应调整用） */
    private int backlogThreshold = 1000;
    
    /** 队列空闲阈值（自适应调整用） */
    private int idleThreshold = 100;

    /**
     * 默认构造（使用默认值）
     */
    public ModeConfig() {
    }

    /**
     * 全参数构造
     *
     * @param batchSize        批量大小
     * @param intervalMs       落地间隔（毫秒）
     * @param adaptiveEnabled  是否启用自适应调整
     * @param backlogThreshold 队列积压阈值
     * @param idleThreshold    队列空闲阈值
     */
    public ModeConfig(int batchSize, long intervalMs, boolean adaptiveEnabled,
                      int backlogThreshold, int idleThreshold) {
        this.batchSize = batchSize;
        this.intervalMs = intervalMs;
        this.adaptiveEnabled = adaptiveEnabled;
        this.backlogThreshold = backlogThreshold;
        this.idleThreshold = idleThreshold;
    }

    /**
     * 简化构造（不启用自适应）
     *
     * @param batchSize  批量大小
     * @param intervalMs 落地间隔（毫秒）
     */
    public ModeConfig(int batchSize, long intervalMs) {
        this.batchSize = batchSize;
        this.intervalMs = intervalMs;
        this.adaptiveEnabled = false;
    }

    // ==================== Getters ====================

    public int getBatchSize() {
        return batchSize;
    }

    public long getIntervalMs() {
        return intervalMs;
    }

    public boolean isAdaptiveEnabled() {
        return adaptiveEnabled;
    }

    public int getBacklogThreshold() {
        return backlogThreshold;
    }

    public int getIdleThreshold() {
        return idleThreshold;
    }

    // ==================== Setters (链式调用) ====================

    public ModeConfig setBatchSize(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive, got: " + batchSize);
        }
        this.batchSize = batchSize;
        return this;
    }

    public ModeConfig setIntervalMs(long intervalMs) {
        if (intervalMs <= 0) {
            throw new IllegalArgumentException("intervalMs must be positive, got: " + intervalMs);
        }
        this.intervalMs = intervalMs;
        return this;
    }

    public ModeConfig setAdaptiveEnabled(boolean adaptiveEnabled) {
        this.adaptiveEnabled = adaptiveEnabled;
        return this;
    }

    public ModeConfig setBacklogThreshold(int backlogThreshold) {
        if (backlogThreshold < 0) {
            throw new IllegalArgumentException("backlogThreshold cannot be negative, got: " + backlogThreshold);
        }
        this.backlogThreshold = backlogThreshold;
        return this;
    }

    public ModeConfig setIdleThreshold(int idleThreshold) {
        if (idleThreshold < 0) {
            throw new IllegalArgumentException("idleThreshold cannot be negative, got: " + idleThreshold);
        }
        this.idleThreshold = idleThreshold;
        return this;
    }

    /**
     * 一次性设置所有参数
     */
    public ModeConfig set(int batchSize, long intervalMs, boolean adaptiveEnabled,
                          int backlogThreshold, int idleThreshold) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive, got: " + batchSize);
        }
        if (intervalMs <= 0) {
            throw new IllegalArgumentException("intervalMs must be positive, got: " + intervalMs);
        }
        if (backlogThreshold < 0) {
            throw new IllegalArgumentException("backlogThreshold cannot be negative, got: " + backlogThreshold);
        }
        if (idleThreshold < 0) {
            throw new IllegalArgumentException("idleThreshold cannot be negative, got: " + idleThreshold);
        }
        this.batchSize = batchSize;
        this.intervalMs = intervalMs;
        this.adaptiveEnabled = adaptiveEnabled;
        this.backlogThreshold = backlogThreshold;
        this.idleThreshold = idleThreshold;
        return this;
    }

    /**
     * 复制配置
     */
    public ModeConfig copy() {
        return new ModeConfig(batchSize, intervalMs, adaptiveEnabled, backlogThreshold, idleThreshold);
    }

    @Override
    public String toString() {
        return String.format("ModeConfig{batch=%d, interval=%dms, adaptive=%s, backlog=%d, idle=%d}",
                batchSize, intervalMs, adaptiveEnabled, backlogThreshold, idleThreshold);
    }
}
