package com.muyi.gameconfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置热更结果
 *
 * @author muyi
 */
public class ReloadResult {
    
    /** 是否成功 */
    private final boolean success;
    
    /** 版本号 */
    private final long version;
    
    /** 耗时(ms) */
    private final long costTime;
    
    /** 成功的配置 */
    private final List<String> successConfigs;
    
    /** 失败的配置 */
    private final List<String> failedConfigs;
    
    /** 错误信息 */
    private final List<String> errors;
    
    private ReloadResult(boolean success, long version, long costTime,
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
    
    /**
     * Builder
     */
    public static class Builder {
        private boolean success = true;
        private long version;
        private long costTime;
        private final List<String> successConfigs = new ArrayList<>();
        private final List<String> failedConfigs = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();
        
        public Builder version(long version) {
            this.version = version;
            return this;
        }
        
        public Builder costTime(long costTime) {
            this.costTime = costTime;
            return this;
        }
        
        public Builder addSuccess(String configName) {
            successConfigs.add(configName);
            return this;
        }
        
        public Builder addFailed(String configName, String error) {
            failedConfigs.add(configName);
            errors.add(configName + ": " + error);
            success = false;
            return this;
        }
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public ReloadResult build() {
            // 创建不可变副本，防止 Builder 后续修改影响结果
            return new ReloadResult(success, version, costTime, 
                    List.copyOf(successConfigs), 
                    List.copyOf(failedConfigs), 
                    List.copyOf(errors));
        }
    }
}
