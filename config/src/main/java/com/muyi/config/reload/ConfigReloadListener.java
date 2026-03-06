package com.muyi.config.reload;

import com.muyi.config.IConfig;

/**
 * 配置热更监听器
 * 用于在配置热更时执行自定义逻辑（如重建业务索引）
 *
 * @author muyi
 */
public interface ConfigReloadListener {

    /**
     * 热更开始前回调
     *
     * @param configClasses 将要热更的配置类
     */
    default void beforeReload(Class<? extends IConfig>[] configClasses) {
    }

    /**
     * 热更完成后回调
     *
     * @param result 热更结果
     */
    default void afterReload(ReloadResult result) {
    }

    /**
     * 单个配置热更完成回调
     *
     * @param configClass 配置类
     * @param success     是否成功
     */
    default void onConfigReloaded(Class<? extends IConfig> configClass, boolean success) {
    }
}
