package com.muyi.rpc.compress;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 压缩器工厂
 *
 * @author muyi
 */
public class CompressorFactory {
    
    private static final Map<Byte, Compressor> COMPRESSORS = new ConcurrentHashMap<>();
    
    /**
     * 默认压缩器类型（默认不压缩）
     */
    private static volatile byte defaultType = Compressor.NONE;
    
    /**
     * 压缩阈值（字节），超过此大小才压缩（默认 1KB）
     */
    private static volatile int compressThreshold = 1024;
    
    static {
        // 注册无压缩（透传）
        register(new NoneCompressor());
        // 注册 Gzip 压缩器
        register(new GzipCompressor());
    }
    
    /**
     * 注册压缩器
     */
    public static void register(Compressor compressor) {
        COMPRESSORS.put(compressor.getType(), compressor);
    }
    
    /**
     * 获取压缩器
     */
    public static Compressor get(byte type) {
        Compressor compressor = COMPRESSORS.get(type);
        if (compressor == null) {
            // 未知类型返回无压缩
            return COMPRESSORS.get(Compressor.NONE);
        }
        return compressor;
    }
    
    /**
     * 获取默认压缩器
     */
    public static Compressor getDefault() {
        return get(defaultType);
    }
    
    /**
     * 设置默认压缩器类型
     * @throws IllegalArgumentException 如果类型未注册
     */
    public static void setDefaultType(byte type) {
        if (type != Compressor.NONE && !COMPRESSORS.containsKey(type)) {
            throw new IllegalArgumentException("Compressor type not registered: " + type);
        }
        defaultType = type;
    }
    
    /**
     * 获取默认压缩器类型
     */
    public static byte getDefaultType() {
        return defaultType;
    }
    
    /**
     * 设置压缩阈值（字节）
     * 数据大小超过此阈值才进行压缩
     */
    public static void setCompressThreshold(int threshold) {
        compressThreshold = threshold;
    }
    
    /**
     * 获取压缩阈值
     */
    public static int getCompressThreshold() {
        return compressThreshold;
    }
    
    /**
     * 判断是否需要压缩
     *
     * @param dataLength 数据长度
     * @return 是否需要压缩
     */
    public static boolean shouldCompress(int dataLength) {
        return defaultType != Compressor.NONE && dataLength >= compressThreshold;
    }
}
