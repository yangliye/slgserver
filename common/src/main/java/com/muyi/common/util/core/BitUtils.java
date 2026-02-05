package com.muyi.common.util.core;

/**
 * 位运算工具类
 * 用于游戏中的状态标记、权限控制等
 */
public final class BitUtils {
    
    private BitUtils() {
        // 工具类禁止实例化
    }
    
    // ==================== 单个位操作 ====================
    
    /**
     * 设置指定位为1
     * @param value 原始值
     * @param bit 位索引（0-63）
     */
    public static long setBit(long value, int bit) {
        return value | (1L << bit);
    }
    
    /**
     * 设置指定位为1
     */
    public static int setBit(int value, int bit) {
        return value | (1 << bit);
    }
    
    /**
     * 清除指定位（设为0）
     * @param value 原始值
     * @param bit 位索引（0-63）
     */
    public static long clearBit(long value, int bit) {
        return value & ~(1L << bit);
    }
    
    /**
     * 清除指定位（设为0）
     */
    public static int clearBit(int value, int bit) {
        return value & ~(1 << bit);
    }
    
    /**
     * 翻转指定位
     * @param value 原始值
     * @param bit 位索引（0-63）
     */
    public static long toggleBit(long value, int bit) {
        return value ^ (1L << bit);
    }
    
    /**
     * 翻转指定位
     */
    public static int toggleBit(int value, int bit) {
        return value ^ (1 << bit);
    }
    
    /**
     * 检查指定位是否为1
     * @param value 原始值
     * @param bit 位索引（0-63）
     */
    public static boolean hasBit(long value, int bit) {
        return (value & (1L << bit)) != 0;
    }
    
    /**
     * 检查指定位是否为1
     */
    public static boolean hasBit(int value, int bit) {
        return (value & (1 << bit)) != 0;
    }
    
    // ==================== 多个位操作 ====================
    
    /**
     * 设置多个位为1
     * @param value 原始值
     * @param bits 要设置的位索引数组
     */
    public static long setBits(long value, int... bits) {
        for (int bit : bits) {
            value = setBit(value, bit);
        }
        return value;
    }
    
    /**
     * 清除多个位
     * @param value 原始值
     * @param bits 要清除的位索引数组
     */
    public static long clearBits(long value, int... bits) {
        for (int bit : bits) {
            value = clearBit(value, bit);
        }
        return value;
    }
    
    /**
     * 检查是否包含所有指定位
     */
    public static boolean hasAllBits(long value, int... bits) {
        for (int bit : bits) {
            if (!hasBit(value, bit)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 检查是否包含任意一个指定位
     */
    public static boolean hasAnyBit(long value, int... bits) {
        for (int bit : bits) {
            if (hasBit(value, bit)) {
                return true;
            }
        }
        return false;
    }
    
    // ==================== 掩码操作 ====================
    
    /**
     * 检查是否包含指定掩码的所有位
     */
    public static boolean hasMask(long value, long mask) {
        return (value & mask) == mask;
    }
    
    /**
     * 检查是否包含指定掩码的任意位
     */
    public static boolean hasAnyMask(long value, long mask) {
        return (value & mask) != 0;
    }
    
    /**
     * 添加掩码
     */
    public static long addMask(long value, long mask) {
        return value | mask;
    }
    
    /**
     * 移除掩码
     */
    public static long removeMask(long value, long mask) {
        return value & ~mask;
    }
    
    // ==================== 计数与查找 ====================
    
    /**
     * 计算值中1的个数
     */
    public static int countBits(long value) {
        return Long.bitCount(value);
    }
    
    /**
     * 计算值中1的个数
     */
    public static int countBits(int value) {
        return Integer.bitCount(value);
    }
    
    /**
     * 获取最低位的1的位置（0-based）
     * 如果值为0，返回-1
     */
    public static int lowestOneBit(long value) {
        if (value == 0) return -1;
        return Long.numberOfTrailingZeros(value);
    }
    
    /**
     * 获取最高位的1的位置（0-based）
     * 如果值为0，返回-1
     */
    public static int highestOneBit(long value) {
        if (value == 0) return -1;
        return 63 - Long.numberOfLeadingZeros(value);
    }
    
    // ==================== 游戏常用场景 ====================
    
    /**
     * 检查功能是否开启
     * @param flags 功能标记位
     * @param feature 功能ID
     */
    public static boolean isFeatureEnabled(long flags, int feature) {
        return hasBit(flags, feature);
    }
    
    /**
     * 开启功能
     */
    public static long enableFeature(long flags, int feature) {
        return setBit(flags, feature);
    }
    
    /**
     * 关闭功能
     */
    public static long disableFeature(long flags, int feature) {
        return clearBit(flags, feature);
    }
    
    /**
     * 检查权限
     * @param permissions 权限位
     * @param permission 需要的权限
     */
    public static boolean hasPermission(long permissions, int permission) {
        return hasBit(permissions, permission);
    }
    
    /**
     * 添加权限
     */
    public static long addPermission(long permissions, int permission) {
        return setBit(permissions, permission);
    }
    
    /**
     * 移除权限
     */
    public static long removePermission(long permissions, int permission) {
        return clearBit(permissions, permission);
    }
    
    /**
     * 检查每日任务是否完成
     * @param dailyFlags 每日任务标记（每位代表一个任务）
     * @param taskId 任务ID（0-63）
     */
    public static boolean isDailyTaskDone(long dailyFlags, int taskId) {
        return hasBit(dailyFlags, taskId);
    }
    
    /**
     * 标记每日任务完成
     */
    public static long markDailyTaskDone(long dailyFlags, int taskId) {
        return setBit(dailyFlags, taskId);
    }
    
    /**
     * 重置所有每日任务
     */
    public static long resetDailyTasks() {
        return 0L;
    }
    
    // ==================== 位段操作 ====================
    
    /**
     * 提取位段
     * @param value 原始值
     * @param start 起始位（低位）
     * @param length 位数
     */
    public static long extractBits(long value, int start, int length) {
        long mask = (1L << length) - 1;
        return (value >> start) & mask;
    }
    
    /**
     * 设置位段
     * @param value 原始值
     * @param start 起始位（低位）
     * @param length 位数
     * @param newValue 要设置的值
     */
    public static long setBits(long value, int start, int length, long newValue) {
        long mask = ((1L << length) - 1) << start;
        return (value & ~mask) | ((newValue << start) & mask);
    }
    
    /**
     * 将两个 int 合并为一个 long
     */
    public static long combineInts(int high, int low) {
        return ((long) high << 32) | (low & 0xFFFFFFFFL);
    }
    
    /**
     * 从 long 中提取高32位
     */
    public static int getHighInt(long value) {
        return (int) (value >> 32);
    }
    
    /**
     * 从 long 中提取低32位
     */
    public static int getLowInt(long value) {
        return (int) value;
    }
    
    /**
     * 将两个 short 合并为一个 int
     */
    public static int combineShorts(short high, short low) {
        return (high << 16) | (low & 0xFFFF);
    }
    
    /**
     * 从 int 中提取高16位
     */
    public static short getHighShort(int value) {
        return (short) (value >> 16);
    }
    
    /**
     * 从 int 中提取低16位
     */
    public static short getLowShort(int value) {
        return (short) value;
    }
}
