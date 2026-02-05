package com.muyi.common.util.core;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机数工具类
 * 基于 ThreadLocalRandom 实现，线程安全且高性能
 */
public final class RandomUtils {
    
    private RandomUtils() {
        // 工具类禁止实例化
    }
    
    /**
     * 获取当前线程的随机数生成器
     */
    public static ThreadLocalRandom random() {
        return ThreadLocalRandom.current();
    }
    
    // ==================== 整数随机 ====================
    
    /**
     * 生成 [0, bound) 范围内的随机整数
     */
    public static int nextInt(int bound) {
        return random().nextInt(bound);
    }
    
    /**
     * 生成 [min, max] 范围内的随机整数（包含两端）
     */
    public static int nextInt(int min, int max) {
        if (min == max) {
            return min;
        }
        if (min > max) {
            throw new IllegalArgumentException("min must <= max");
        }
        // 防止 max + 1 溢出
        if (max == Integer.MAX_VALUE) {
            return min + random().nextInt(max - min + 1);
        }
        return random().nextInt(min, max + 1);
    }
    
    /**
     * 生成 [0, bound) 范围内的随机长整数
     */
    public static long nextLong(long bound) {
        return random().nextLong(bound);
    }
    
    /**
     * 生成 [min, max] 范围内的随机长整数（包含两端）
     */
    public static long nextLong(long min, long max) {
        if (min == max) {
            return min;
        }
        if (min > max) {
            throw new IllegalArgumentException("min must <= max");
        }
        // 防止 max + 1 溢出
        if (max == Long.MAX_VALUE) {
            // 特殊处理：随机决定是否返回 max
            long result = random().nextLong(min, max);
            return random().nextBoolean() ? result : max;
        }
        return random().nextLong(min, max + 1);
    }
    
    // ==================== 浮点数随机 ====================
    
    /**
     * 生成 [0.0, 1.0) 范围内的随机浮点数
     */
    public static double nextDouble() {
        return random().nextDouble();
    }
    
    /**
     * 生成 [0.0, bound) 范围内的随机浮点数
     */
    public static double nextDouble(double bound) {
        return random().nextDouble(bound);
    }
    
    /**
     * 生成 [min, max) 范围内的随机浮点数
     */
    public static double nextDouble(double min, double max) {
        return random().nextDouble(min, max);
    }
    
    // ==================== 概率判断 ====================
    
    /**
     * 按概率判断是否命中
     * @param probability 概率，范围 [0.0, 1.0]，例如 0.3 表示 30% 概率
     * @return true 表示命中
     */
    public static boolean probability(double probability) {
        if (probability <= 0) {
            return false;
        }
        if (probability >= 1) {
            return true;
        }
        return nextDouble() < probability;
    }
    
    /**
     * 按万分比判断是否命中
     * @param rate 万分比，例如 3000 表示 30% 概率
     * @return true 表示命中
     */
    public static boolean rate10000(int rate) {
        if (rate <= 0) {
            return false;
        }
        if (rate >= 10000) {
            return true;
        }
        return nextInt(10000) < rate;
    }
    
    /**
     * 按百分比判断是否命中
     * @param percent 百分比，例如 30 表示 30% 概率
     * @return true 表示命中
     */
    public static boolean percent(int percent) {
        if (percent <= 0) {
            return false;
        }
        if (percent >= 100) {
            return true;
        }
        return nextInt(100) < percent;
    }
    
    // ==================== 集合随机 ====================
    
    /**
     * 从列表中随机选择一个元素
     */
    public static <T> T randomElement(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(nextInt(list.size()));
    }
    
    /**
     * 从数组中随机选择一个元素
     */
    public static <T> T randomElement(T[] array) {
        if (array == null || array.length == 0) {
            return null;
        }
        return array[nextInt(array.length)];
    }
    
    /**
     * 从整数数组中随机选择一个元素
     */
    public static int randomElement(int[] array) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException("array is empty");
        }
        return array[nextInt(array.length)];
    }
    
    // ==================== 权重随机 ====================
    
    /**
     * 按权重随机选择索引
     * @param weights 权重数组（所有权重必须 >= 0）
     * @return 选中的索引
     */
    public static int weightedIndex(int[] weights) {
        if (weights == null || weights.length == 0) {
            throw new IllegalArgumentException("weights is empty");
        }
        
        int totalWeight = 0;
        for (int weight : weights) {
            if (weight < 0) {
                throw new IllegalArgumentException("weight cannot be negative: " + weight);
            }
            totalWeight += weight;
        }
        
        // 所有权重为 0 时，均匀随机选择
        if (totalWeight == 0) {
            return nextInt(weights.length);
        }
        
        int randomValue = nextInt(totalWeight);
        int cumulative = 0;
        
        for (int i = 0; i < weights.length; i++) {
            cumulative += weights[i];
            if (randomValue < cumulative) {
                return i;
            }
        }
        
        return weights.length - 1;
    }
    
    /**
     * 按权重随机选择元素
     * @param elements 元素列表
     * @param weights 权重列表（与元素一一对应）
     * @return 选中的元素
     */
    public static <T> T weightedElement(List<T> elements, int[] weights) {
        if (elements == null || elements.isEmpty()) {
            return null;
        }
        if (weights == null || weights.length != elements.size()) {
            throw new IllegalArgumentException("weights size must match elements size");
        }
        
        int index = weightedIndex(weights);
        return elements.get(index);
    }
    
    // ==================== 字符串随机 ====================
    
    private static final String DIGITS = "0123456789";
    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String ALPHANUMERIC = DIGITS + LETTERS;
    
    /**
     * 生成指定长度的随机数字字符串
     */
    public static String randomDigits(int length) {
        return randomString(length, DIGITS);
    }
    
    /**
     * 生成指定长度的随机字母字符串
     */
    public static String randomLetters(int length) {
        return randomString(length, LETTERS);
    }
    
    /**
     * 生成指定长度的随机字母数字字符串
     */
    public static String randomAlphanumeric(int length) {
        return randomString(length, ALPHANUMERIC);
    }
    
    /**
     * 从指定字符集中生成随机字符串
     */
    public static String randomString(int length, String chars) {
        if (length <= 0) {
            return "";
        }
        if (chars == null || chars.isEmpty()) {
            throw new IllegalArgumentException("chars is empty");
        }
        
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(nextInt(chars.length())));
        }
        return sb.toString();
    }
}
