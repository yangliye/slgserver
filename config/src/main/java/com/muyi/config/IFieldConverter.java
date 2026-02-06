package com.muyi.config;

/**
 * 字段转换器接�?
 * 用于�?XML 属性字符串转换为复杂类�?
 *
 * @param <T> 目标类型
 * @author muyi
 */
public interface IFieldConverter<T> {
    
    /**
     * 将字符串转换为目标类�?
     * 
     * @param value 原始字符串�?
     * @return 转换后的对象
     */
    T convert(String value);
    
    /**
     * 获取默认值（当原始值为空时使用�?
     * 
     * @return 默认值，可以�?null
     */
    default T defaultValue() {
        return null;
    }
}
