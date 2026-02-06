package com.muyi.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 配置字段转换器注�?
 * 用于标记需要自定义解析的字�?
 * 
 * 使用示例�?
 * <pre>{@code
 * public class UnitConfig implements IConfig {
 *     private int id;
 *     
 *     // 原始字符�?"1,2,3" 转换�?List<Integer>
 *     @ConfigConverter(IntListConverter.class)
 *     private List<Integer> unlockLevels;
 *     
 *     // 原始字符�?"100:10,200:20" 转换�?Map
 *     @ConfigConverter(RewardMapConverter.class)
 *     private Map<Integer, Integer> rewards;
 * }
 * }</pre>
 *
 * @author muyi
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigConverter {
    
    /**
     * 转换器类
     */
    Class<? extends IFieldConverter<?>> value();
}
