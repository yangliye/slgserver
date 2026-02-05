package com.muyi.db.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.muyi.db.DbManager;

/**
 * 场景化数据库配置管理器
 * <p>
 * 根据游戏不同场景动态调整数据库落地参数，平衡吞吐量与延迟。
 * 所有配置参数均可自定义修改，适合作为工具包使用。
 * <p>
 * 内置四种模式（可自定义参数）：
 * <ul>
 *   <li>日常模式（NORMAL）：普通在线玩家操作</li>
 *   <li>高峰模式（PEAK）：跨天结算、活动开启等高负载场景</li>
 *   <li>极限模式（EXTREME）：大规模战斗、紧急情况</li>
 *   <li>节能模式（IDLE）：凌晨低在线时段</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 基础用法
 * DbManager dbManager = new DbManager(config);
 * SceneDbConfigManager configManager = new SceneDbConfigManager(dbManager);
 * configManager.switchToNormalMode();
 * 
 * // 自定义配置
 * configManager.getNormalConfig()
 *     .setBatchSize(500)
 *     .setIntervalMs(20);
 * 
 * // 或者链式配置
 * configManager
 *     .normalConfig(500, 20, true, 2000, 200)
 *     .peakConfig(800, 10, false)
 *     .switchToNormalMode();
 * }</pre>
 */
public class SLGDbConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(SLGDbConfigManager.class);

    private final DbManager dbManager;

    /**
     * 当前模式
     */
    private volatile ConfigMode currentMode = ConfigMode.NORMAL;

    /**
     * 配置模式枚举
     */
    public enum ConfigMode {
        /** 日常模式 */
        NORMAL,
        /** 高峰模式（跨天、活动等） */
        PEAK,
        /** 极限模式（大规模战斗） */
        EXTREME,
        /** 低谷节能模式 */
        IDLE
    }

    // ==================== 各模式配置对象 ====================

    private final ModeConfig normalConfig = new ModeConfig(400, 25, true, 2000, 200);
    private final ModeConfig peakConfig = new ModeConfig(600, 15, false, 0, 0);
    private final ModeConfig extremeConfig = new ModeConfig(800, 10, false, 0, 0);
    private final ModeConfig idleConfig = new ModeConfig(200, 100, true, 500, 50);

    public SLGDbConfigManager(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    // ==================== 获取配置对象（可修改） ====================

    /**
     * 获取日常模式配置（可修改）
     */
    public ModeConfig getNormalConfig() {
        return normalConfig;
    }

    /**
     * 获取高峰模式配置（可修改）
     */
    public ModeConfig getPeakConfig() {
        return peakConfig;
    }

    /**
     * 获取极限模式配置（可修改）
     */
    public ModeConfig getExtremeConfig() {
        return extremeConfig;
    }

    /**
     * 获取节能模式配置（可修改）
     */
    public ModeConfig getIdleConfig() {
        return idleConfig;
    }

    // ==================== 链式配置方法 ====================

    /**
     * 配置日常模式参数
     */
    public SLGDbConfigManager normalConfig(int batchSize, long intervalMs, boolean adaptive,
                                            int backlogThreshold, int idleThreshold) {
        normalConfig.set(batchSize, intervalMs, adaptive, backlogThreshold, idleThreshold);
        return this;
    }

    /**
     * 配置高峰模式参数
     */
    public SLGDbConfigManager peakConfig(int batchSize, long intervalMs, boolean adaptive) {
        peakConfig.setBatchSize(batchSize).setIntervalMs(intervalMs).setAdaptiveEnabled(adaptive);
        return this;
    }

    /**
     * 配置极限模式参数
     */
    public SLGDbConfigManager extremeConfig(int batchSize, long intervalMs, boolean adaptive) {
        extremeConfig.setBatchSize(batchSize).setIntervalMs(intervalMs).setAdaptiveEnabled(adaptive);
        return this;
    }

    /**
     * 配置节能模式参数
     */
    public SLGDbConfigManager idleConfig(int batchSize, long intervalMs, boolean adaptive,
                                          int backlogThreshold, int idleThreshold) {
        idleConfig.set(batchSize, intervalMs, adaptive, backlogThreshold, idleThreshold);
        return this;
    }

    // ==================== 模式切换方法 ====================

    /**
     * 切换到日常模式
     * <p>
     * 适用：普通在线玩家操作
     */
    public SLGDbConfigManager switchToNormalMode() {
        return applyConfig(ConfigMode.NORMAL, normalConfig);
    }

    /**
     * 切换到高峰模式
     * <p>
     * 适用：跨天结算、活动开启等高负载场景
     */
    public SLGDbConfigManager switchToPeakMode() {
        return applyConfig(ConfigMode.PEAK, peakConfig);
    }

    /**
     * 切换到极限模式
     * <p>
     * 适用：大规模战斗、紧急情况，追求最大吞吐量
     */
    public SLGDbConfigManager switchToExtremeMode() {
        return applyConfig(ConfigMode.EXTREME, extremeConfig);
    }

    /**
     * 切换到节能模式
     * <p>
     * 适用：凌晨低在线时段
     */
    public SLGDbConfigManager switchToIdleMode() {
        return applyConfig(ConfigMode.IDLE, idleConfig);
    }

    /**
     * 根据模式枚举切换
     */
    public SLGDbConfigManager switchToMode(ConfigMode mode) {
        return switch (mode) {
            case NORMAL -> switchToNormalMode();
            case PEAK -> switchToPeakMode();
            case EXTREME -> switchToExtremeMode();
            case IDLE -> switchToIdleMode();
        };
    }

    /**
     * 应用配置到 DbManager
     */
    private SLGDbConfigManager applyConfig(ConfigMode mode, ModeConfig config) {
        if (currentMode == mode) {
            logger.debug("Already in {} mode, skip", mode);
            return this;
        }

        doApplyConfig(config);
        currentMode = mode;
        logger.info("Switched to {} mode: {}", mode, config);
        return this;
    }

    /**
     * 应用自定义配置（不改变模式名称）
     */
    public SLGDbConfigManager applyCustomConfig(ModeConfig config) {
        doApplyConfig(config);
        logger.info("Applied custom config: {}", config);
        return this;
    }
    
    /**
     * 实际应用配置到 DbManager（内部方法）
     */
    private void doApplyConfig(ModeConfig config) {
        dbManager.setLandBatchSize(config.getBatchSize());
        dbManager.setLandIntervalMs(config.getIntervalMs());
        dbManager.setAdaptiveEnabled(config.isAdaptiveEnabled());
        
        if (config.isAdaptiveEnabled()) {
            dbManager.setBacklogThreshold(config.getBacklogThreshold());
            dbManager.setIdleThreshold(config.getIdleThreshold());
        }
    }

    // ==================== 状态查询 ====================

    /**
     * 获取当前模式
     */
    public ConfigMode getCurrentMode() {
        return currentMode;
    }

    /**
     * 获取当前模式名称
     */
    public String getCurrentModeName() {
        return currentMode.name();
    }

    /**
     * 获取指定模式的配置
     */
    public ModeConfig getConfig(ConfigMode mode) {
        return switch (mode) {
            case NORMAL -> normalConfig;
            case PEAK -> peakConfig;
            case EXTREME -> extremeConfig;
            case IDLE -> idleConfig;
        };
    }

    // ==================== 监控辅助 ====================

    /**
     * 获取当前状态摘要（用于监控/日志）
     */
    public String getStatusSummary() {
        return String.format(
                "Mode=%s, Pending=%d, Success=%d, Failed=%d, Retry=%d",
                currentMode,
                dbManager.getPendingLandTasks(),
                dbManager.getSuccessLandTasks(),
                dbManager.getFailedLandTasks(),
                dbManager.getLandRetryCount()
        );
    }

    /**
     * 检查是否需要告警（队列积压过多）
     * 
     * @param threshold 告警阈值
     * @return 是否需要告警
     */
    public boolean needAlert(int threshold) {
        return dbManager.getPendingLandTasks() > threshold;
    }

    /**
     * 获取待处理任务数
     */
    public int getPendingTasks() {
        return dbManager.getPendingLandTasks();
    }
}
