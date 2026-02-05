package com.muyi.rpc.serialize;

/**
 * 序列化器接口
 *
 * @author muyi
 */
public interface Serializer {
    
    /**
     * 序列化
     *
     * @param obj 待序列化对象
     * @return 字节数组
     */
    byte[] serialize(Object obj);
    
    /**
     * 反序列化
     *
     * @param bytes 字节数组
     * @param clazz 目标类型
     * @param <T>   泛型
     * @return 反序列化对象
     */
    <T> T deserialize(byte[] bytes, Class<T> clazz);
    
    /**
     * 获取序列化器类型
     */
    byte getType();
}

