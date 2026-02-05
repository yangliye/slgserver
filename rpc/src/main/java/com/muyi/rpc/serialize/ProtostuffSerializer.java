package com.muyi.rpc.serialize;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

/**
 * Protostuff序列化器 - 高性能序列化
 * 比Java原生序列化快10倍以上，体积更小
 *
 * @author muyi
 */
public class ProtostuffSerializer implements Serializer {
    
    public static final byte TYPE = 1;
    
    /**
     * Schema缓存，避免重复创建
     */
    private static final Map<Class<?>, Schema<?>> SCHEMA_CACHE = new ConcurrentHashMap<>();
    
    /**
     * 默认缓冲区大小
     */
    private static final int BUFFER_SIZE = LinkedBuffer.DEFAULT_BUFFER_SIZE;
    
    @Override
    @SuppressWarnings("unchecked")
    public byte[] serialize(Object obj) {
        if (obj == null) {
            return new byte[0];
        }
        
        Class<?> clazz = obj.getClass();
        Schema<Object> schema = (Schema<Object>) getSchema(clazz);
        // 每次分配新的 buffer，避免虚拟线程环境下 ThreadLocal 内存泄漏
        // LinkedBuffer 本身很轻量（默认512字节），分配开销可接受
        LinkedBuffer buffer = LinkedBuffer.allocate(BUFFER_SIZE);
        
        try {
            return ProtostuffIOUtil.toByteArray(obj, schema, buffer);
        } finally {
            buffer.clear();
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        
        Schema<T> schema = getSchema(clazz);
        T obj = schema.newMessage();
        ProtostuffIOUtil.mergeFrom(bytes, obj, schema);
        return obj;
    }
    
    @Override
    public byte getType() {
        return TYPE;
    }
    
    /**
     * 获取或创建Schema（带缓存）
     */
    @SuppressWarnings("unchecked")
    private <T> Schema<T> getSchema(Class<T> clazz) {
        return (Schema<T>) SCHEMA_CACHE.computeIfAbsent(clazz, RuntimeSchema::createFrom);
    }
}

