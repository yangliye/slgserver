package com.muyi.rpc.serialize;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 序列化器工厂
 *
 * @author muyi
 */
public class SerializerFactory {
    
    private static final Map<Byte, Serializer> SERIALIZERS = new ConcurrentHashMap<>();
    
    /**
     * 默认序列化器类型
     */
    private static volatile byte defaultType = ProtostuffSerializer.TYPE;
    
    static {
        // 注册Protostuff序列化器
        register(new ProtostuffSerializer());
    }
    
    /**
     * 注册序列化器
     */
    public static void register(Serializer serializer) {
        SERIALIZERS.put(serializer.getType(), serializer);
    }
    
    /**
     * 获取序列化器
     */
    public static Serializer get(byte type) {
        Serializer serializer = SERIALIZERS.get(type);
        if (serializer == null) {
            throw new IllegalArgumentException("Unknown serializer type: " + type);
        }
        return serializer;
    }
    
    /**
     * 设置默认序列化器类型
     * @throws IllegalArgumentException 如果类型未注册
     */
    public static void setDefaultType(byte type) {
        if (!SERIALIZERS.containsKey(type)) {
            throw new IllegalArgumentException("Serializer type not registered: " + type);
        }
        defaultType = type;
    }
    
    /**
     * 获取默认序列化器类型
     */
    public static byte getDefaultType() {
        return defaultType;
    }
}

