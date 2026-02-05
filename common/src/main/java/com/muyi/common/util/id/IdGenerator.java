package com.muyi.common.util.id;

import com.muyi.common.util.time.TimeUtils;

import java.util.concurrent.atomic.AtomicLong;

/**
 * ID 生成器
 * 支持雪花算法和简单自增 ID
 */
public final class IdGenerator {
    
    // ==================== 雪花算法相关常量 ====================
    
    /** 起始时间戳 (2024-01-01 00:00:00) */
    private static final long EPOCH = 1704067200000L;
    
    /** 服务器ID占用位数（12位，支持4096台服务器） */
    private static final long SERVER_ID_BITS = 12L;
    /** 序列号占用位数（10位，每毫秒1024个ID） */
    private static final long SEQUENCE_BITS = 10L;
    
    /** 最大服务器ID = 4095 */
    private static final long MAX_SERVER_ID = ~(-1L << SERVER_ID_BITS);
    /** 序列号掩码 = 1023 */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);
    
    /** 服务器ID左移位数 */
    private static final long SERVER_ID_SHIFT = SEQUENCE_BITS;
    /** 时间戳左移位数 */
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + SERVER_ID_BITS;
    
    /** 默认实例 */
    private static IdGenerator defaultInstance = new IdGenerator(0);
    
    /** 服务器ID (0-4095) */
    private final long serverId;
    /** 序列号 */
    private long sequence = 0L;
    /** 上次生成ID的时间戳 */
    private long lastTimestamp = -1L;
    
    // ==================== 简单自增ID ====================
    
    /** 简单自增ID计数器 */
    private static final AtomicLong SIMPLE_ID = new AtomicLong(0);
    
    /**
     * 构造函数
     * @param serverId 服务器ID (0-4095)
     */
    public IdGenerator(long serverId) {
        if (serverId > MAX_SERVER_ID || serverId < 0) {
            throw new IllegalArgumentException("Server ID must be between 0 and " + MAX_SERVER_ID);
        }
        this.serverId = serverId;
    }
    
    /**
     * 获取默认实例
     */
    public static IdGenerator getDefault() {
        return defaultInstance;
    }
    
    /**
     * 初始化默认实例
     * @param serverId 服务器ID (0-4095)
     */
    public static void initDefault(long serverId) {
        defaultInstance = new IdGenerator(serverId);
    }
    
    /**
     * 生成下一个雪花ID
     */
    public synchronized long nextId() {
        long timestamp = currentTime();
        
        // 时钟回拨检测
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id for " 
                    + (lastTimestamp - timestamp) + " milliseconds");
        }
        
        // 同一毫秒内
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            // 序列号溢出，等待下一毫秒
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        
        lastTimestamp = timestamp;
        
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (serverId << SERVER_ID_SHIFT)
                | sequence;
    }
    
    /**
     * 使用默认实例生成雪花ID
     */
    public static long snowflakeId() {
        return getDefault().nextId();
    }
    
    /**
     * 解析雪花ID
     * @return [时间戳, 服务器ID, 序列号]
     */
    public static long[] parseSnowflakeId(long id) {
        long[] result = new long[3];
        result[0] = (id >> TIMESTAMP_SHIFT) + EPOCH;  // 时间戳
        result[1] = (id >> SERVER_ID_SHIFT) & MAX_SERVER_ID;  // 服务器ID
        result[2] = id & SEQUENCE_MASK;  // 序列号
        return result;
    }
    
    /**
     * 获取雪花ID中的时间戳
     */
    public static long getTimestamp(long id) {
        return (id >> TIMESTAMP_SHIFT) + EPOCH;
    }
    
    /**
     * 获取雪花ID中的服务器ID
     */
    public static long getServerId(long id) {
        return (id >> SERVER_ID_SHIFT) & MAX_SERVER_ID;
    }
    
    /**
     * 获取最大支持的服务器数量
     */
    public static long getMaxServerCount() {
        return MAX_SERVER_ID + 1;  // 4096
    }
    
    /**
     * 获取每毫秒最大ID数量
     */
    public static long getMaxSequencePerMs() {
        return SEQUENCE_MASK + 1;  // 1024
    }
    
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = currentTime();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTime();
        }
        return timestamp;
    }
    
    private long currentTime() {
        return TimeUtils.currentTimeMillis();
    }
    
    // ==================== 简单自增ID ====================
    
    /**
     * 生成简单自增ID（从1开始）
     */
    public static long simpleId() {
        return SIMPLE_ID.incrementAndGet();
    }
    
    /**
     * 重置简单自增ID
     */
    public static void resetSimpleId() {
        SIMPLE_ID.set(0);
    }
    
    /**
     * 设置简单自增ID的起始值
     */
    public static void setSimpleIdStart(long start) {
        SIMPLE_ID.set(start);
    }
    
    // ==================== 组合ID ====================
    
    /**
     * 生成组合ID（高32位为类型，低32位为自增）
     * 适用于区分不同类型的实体ID
     */
    public static long combineId(int type, int sequence) {
        return ((long) type << 32) | (sequence & 0xFFFFFFFFL);
    }
    
    /**
     * 从组合ID中获取类型
     */
    public static int getTypeFromCombineId(long id) {
        return (int) (id >> 32);
    }
    
    /**
     * 从组合ID中获取序列号
     */
    public static int getSequenceFromCombineId(long id) {
        return (int) (id & 0xFFFFFFFFL);
    }
    
    // ==================== 玩家ID生成 ====================
    
    /**
     * 生成玩家ID（服务器ID + 自增序列）
     * 格式：SSSSNNNNNNNN（4位服务器ID + 8位序列号）
     * @param serverId 服务器ID (1-9999)
     * @param sequence 序列号 (1-99999999)
     */
    public static long playerId(int serverId, int sequence) {
        if (serverId < 1 || serverId > 9999) {
            throw new IllegalArgumentException("Server ID must be between 1 and 9999");
        }
        if (sequence < 1 || sequence > 99999999) {
            throw new IllegalArgumentException("Sequence must be between 1 and 99999999");
        }
        return (long) serverId * 100000000L + sequence;
    }
    
    /**
     * 从玩家ID中获取服务器ID
     */
    public static int getServerIdFromPlayerId(long playerId) {
        return (int) (playerId / 100000000L);
    }
    
    /**
     * 从玩家ID中获取序列号
     */
    public static int getSequenceFromPlayerId(long playerId) {
        return (int) (playerId % 100000000L);
    }
}
