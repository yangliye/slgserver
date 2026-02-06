package com.muyi.config;

import java.util.Map;

/**
 * 配置接口
 * 所有策划配置类需要实现此接口
 *
 * @author muyi
 */
public interface IConfig {
    
    /**
     * 获取配置ID
     * 每条配置的唯一标识
     */
    int getId();
    
    /**
     * 配置加载完成后的回调（生命周期方法）
     * 用于自定义字段组装、数据校验、缓存预热等
     * 
     * @param rawAttributes 原始 XML 属性（key=属性名, value=属性值字符串�?
     */
    default void afterLoad(Map<String, String> rawAttributes) {
        // 默认空实现，子类可覆�?
    }
    
    /**
     * 数据校验
     * �?afterLoad 之后调用，用于校验配置数据是否合�?
     * 
     * @throws IllegalStateException 如果数据不合�?
     */
    default void validate() {
        // 默认空实现，子类可覆�?
    }
}
