package com.muyi.gameconfig;

/**
 * 字段转换器接口
 * 用于将 XML 属性字符串转换为复杂类型
 *
 * @param <T> 目标类型
 * @author muyi
 */
public interface IFieldConverter<T> {
    
    /**
     * 将字符串转换为目标类型
     * 
     * @param value 原始字符串值
     * @return 转换后的对象
     */
    T convert(String value);
    
    /**
     * 获取默认值（当原始值为空时使用）
     * 
     * @return 默认值，可以为 null
     */
    default T defaultValue() {
        return null;
    }
}
