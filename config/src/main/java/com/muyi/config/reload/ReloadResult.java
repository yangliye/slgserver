package com.muyi.config.reload;

import java.util.List;

/**
 * 配置热更结果
 *
 * @author muyi
 */
public class ReloadResult {

    private final boolean success;
    private final long version;
    private final long costTime;
    private final List<String> successConfigs;
    private final List<String> failedConfigs;
    private final List<String> errors;

    ReloadResult(boolean success, long version, long costTime,
                 List<String> successConfigs, List<String> failedConfigs,
                 List<String> errors) {
        this.success = success;
        this.version = version;
        this.costTime = costTime;
        this.successConfigs = successConfigs;
        this.failedConfigs = failedConfigs;
        this.errors = errors;
    }

    public boolean isSuccess() {
        return success;
    }

    public long getVersion() {
        return version;
    }

    public long getCostTime() {
        return costTime;
    }

    public List<String> getSuccessConfigs() {
        return successConfigs;
    }

    public List<String> getFailedConfigs() {
        return failedConfigs;
    }

    public List<String> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        return "ReloadResult{" +
                "success=" + success +
                ", version=" + version +
                ", costTime=" + costTime + "ms" +
                ", successCount=" + successConfigs.size() +
                ", failedCount=" + failedConfigs.size() +
                '}';
    }
}
