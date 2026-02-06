package com.muyi.gameconfig;

/**
 * 配置热更监听器
 * 用于在配置热更时执行自定义逻辑（如重建业务索引）
 * 
 * 注意：gameconfig 在所有 module 之前启动，首次加载无需监听，
 * 业务模块在自己的 start() 中直接构建索引即可。
 * 
 * 使用示例：
 * <pre>{@code
 * ConfigManager.getInstance().addReloadListener(new ConfigReloadListener() {
 *     @Override
 *     public void onConfigReloaded(Class<? extends IConfig> configClass, boolean success) {
 *         if (success && configClass == UnitConfig.class) {
 *             // 热更时重建业务索引
 *             UnitManager.getInstance().rebuildIndex();
 *         }
 *     }
 * });
 * }</pre>
 *
 * @author muyi
 */
public interface ConfigReloadListener {
    
    /**
     * 热更开始前回调
     * 
     * @param configClasses 将要热更的配置类
     */
    default void beforeReload(Class<? extends IConfig>[] configClasses) {}
    
    /**
     * 热更完成后回调
     * 
     * @param result 热更结果
     */
    default void afterReload(ReloadResult result) {}
    
    /**
     * 单个配置热更完成回调
     * 
     * @param configClass 配置类
     * @param success 是否成功
     */
    default void onConfigReloaded(Class<? extends IConfig> configClass, boolean success) {}
}
