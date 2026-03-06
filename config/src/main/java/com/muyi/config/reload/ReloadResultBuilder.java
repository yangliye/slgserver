package com.muyi.config.reload;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link ReloadResult} 构建器
 *
 * @author muyi
 */
public class ReloadResultBuilder {

    private boolean success = true;
    private long version;
    private long costTime;
    private final List<String> successConfigs = new ArrayList<>();
    private final List<String> failedConfigs = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    public ReloadResultBuilder version(long version) {
        this.version = version;
        return this;
    }

    public ReloadResultBuilder costTime(long costTime) {
        this.costTime = costTime;
        return this;
    }

    public ReloadResultBuilder addSuccess(String configName) {
        successConfigs.add(configName);
        return this;
    }

    public ReloadResultBuilder addFailed(String configName, String error) {
        failedConfigs.add(configName);
        errors.add(configName + ": " + error);
        success = false;
        return this;
    }

    public ReloadResultBuilder success(boolean success) {
        this.success = success;
        return this;
    }

    public ReloadResult build() {
        return new ReloadResult(success, version, costTime,
                List.copyOf(successConfigs),
                List.copyOf(failedConfigs),
                List.copyOf(errors));
    }
}
