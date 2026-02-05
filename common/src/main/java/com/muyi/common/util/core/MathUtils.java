package com.muyi.common.util.core;

/**
 * 数学工具类
 * 提供游戏中常用的数学计算方法
 */
public final class MathUtils {
    
    private MathUtils() {
        // 工具类禁止实例化
    }
    
    // ==================== 范围限制 ====================
    
    /**
     * 将值限制在指定范围内
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * 将值限制在指定范围内
     */
    public static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * 将值限制在指定范围内
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * 将值限制在 [0, max] 范围内
     */
    public static int clampPositive(int value, int max) {
        return clamp(value, 0, max);
    }
    
    /**
     * 将值限制为非负数
     */
    public static int nonNegative(int value) {
        return Math.max(0, value);
    }
    
    /**
     * 将值限制为非负数
     */
    public static long nonNegative(long value) {
        return Math.max(0, value);
    }
    
    // ==================== 安全运算（防溢出）====================
    
    /**
     * 安全加法，防止溢出
     */
    public static int safeAdd(int a, int b) {
        long result = (long) a + b;
        return (int) clamp(result, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
    
    /**
     * 安全加法，防止溢出
     */
    public static long safeAdd(long a, long b) {
        try {
            return Math.addExact(a, b);
        } catch (ArithmeticException e) {
            return (a > 0) ? Long.MAX_VALUE : Long.MIN_VALUE;
        }
    }
    
    /**
     * 安全乘法，防止溢出
     */
    public static int safeMultiply(int a, int b) {
        long result = (long) a * b;
        return (int) clamp(result, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
    
    /**
     * 安全乘法，防止溢出
     */
    public static long safeMultiply(long a, long b) {
        try {
            return Math.multiplyExact(a, b);
        } catch (ArithmeticException e) {
            boolean positive = (a > 0) == (b > 0);
            return positive ? Long.MAX_VALUE : Long.MIN_VALUE;
        }
    }
    
    // ==================== 百分比计算 ====================
    
    /**
     * 计算百分比：value * percent / 100
     */
    public static int percent(int value, int percent) {
        return (int) ((long) value * percent / 100);
    }
    
    /**
     * 计算万分比：value * rate / 10000
     */
    public static int rate10000(int value, int rate) {
        return (int) ((long) value * rate / 10000);
    }
    
    /**
     * 计算百分比增益后的值：value * (100 + percent) / 100
     */
    public static int addPercent(int value, int percent) {
        return (int) ((long) value * (100 + percent) / 100);
    }
    
    /**
     * 计算万分比增益后的值：value * (10000 + rate) / 10000
     */
    public static int addRate10000(int value, int rate) {
        return (int) ((long) value * (10000 + rate) / 10000);
    }
    
    // ==================== 距离计算 ====================
    
    /**
     * 计算两点之间的欧几里得距离
     */
    public static double distance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * 计算两点之间的曼哈顿距离
     */
    public static int manhattanDistance(int x1, int y1, int x2, int y2) {
        return Math.abs(x2 - x1) + Math.abs(y2 - y1);
    }
    
    /**
     * 计算两点之间的切比雪夫距离（棋盘距离）
     */
    public static int chebyshevDistance(int x1, int y1, int x2, int y2) {
        return Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
    }
    
    /**
     * 计算两点之间距离的平方（避免开方运算）
     */
    public static int distanceSquared(int x1, int y1, int x2, int y2) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        return dx * dx + dy * dy;
    }
    
    /**
     * 判断两点是否在指定范围内
     */
    public static boolean inRange(int x1, int y1, int x2, int y2, int range) {
        return distanceSquared(x1, y1, x2, y2) <= range * range;
    }
    
    // ==================== 整除与取模 ====================
    
    /**
     * 向上整除（仅适用于正数）
     * 若需要支持负数，请使用 Math.ceilDiv (JDK 18+)
     */
    public static int divCeil(int dividend, int divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("divisor cannot be zero");
        }
        // 仅对正数有效的向上整除
        if (dividend <= 0 || divisor < 0) {
            // 使用精确的向上取整逻辑
            return (int) Math.ceil((double) dividend / divisor);
        }
        return (dividend + divisor - 1) / divisor;
    }
    
    /**
     * 向上整除（仅适用于正数）
     * 若需要支持负数，请使用 Math.ceilDiv (JDK 18+)
     */
    public static long divCeil(long dividend, long divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("divisor cannot be zero");
        }
        // 仅对正数有效的向上整除
        if (dividend <= 0 || divisor < 0) {
            // 使用精确的向上取整逻辑
            return (long) Math.ceil((double) dividend / divisor);
        }
        return (dividend + divisor - 1) / divisor;
    }
    
    /**
     * 向下取整到指定精度
     * 例如：floorTo(1234, 100) = 1200
     */
    public static int floorTo(int value, int precision) {
        return (value / precision) * precision;
    }
    
    /**
     * 向上取整到指定精度
     * 例如：ceilTo(1234, 100) = 1300
     */
    public static int ceilTo(int value, int precision) {
        return divCeil(value, precision) * precision;
    }
    
    // ==================== 位运算 ====================
    
    /**
     * 判断是否是2的幂
     */
    public static boolean isPowerOfTwo(int value) {
        return value > 0 && (value & (value - 1)) == 0;
    }
    
    /**
     * 获取大于等于 value 的最小2的幂
     * 如果结果超过 2^30，返回 2^30（int 范围内最大的2的幂）
     */
    public static int nextPowerOfTwo(int value) {
        if (value <= 0) return 1;
        if (value > (1 << 30)) return 1 << 30;  // 防止溢出
        value--;
        value |= value >> 1;
        value |= value >> 2;
        value |= value >> 4;
        value |= value >> 8;
        value |= value >> 16;
        return value + 1;
    }
    
    // ==================== 线性插值 ====================
    
    /**
     * 线性插值
     * @param start 起始值
     * @param end 结束值
     * @param t 插值系数 [0, 1]
     */
    public static double lerp(double start, double end, double t) {
        return start + (end - start) * t;
    }
    
    /**
     * 反向线性插值，计算 value 在 [start, end] 范围内的位置
     * @return [0, 1] 范围的值
     */
    public static double inverseLerp(double start, double end, double value) {
        if (Math.abs(end - start) < 1e-10) {
            return 0;
        }
        return (value - start) / (end - start);
    }
}
