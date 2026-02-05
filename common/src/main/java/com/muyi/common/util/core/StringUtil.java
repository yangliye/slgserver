package com.muyi.common.util.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 字符串工具类
 * 提供游戏中常用的字符串处理方法
 */
public final class StringUtil {
    
    /** 数字正则 */
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^-?\\d+$");
    /** 邮箱正则 */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.\\w+$");
    /** 手机号正则（中国大陆） */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    
    private StringUtil() {
        // 工具类禁止实例化
    }
    
    // ==================== 空判断 ====================
    
    /**
     * 判断字符串是否为空（null 或空字符串）
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }
    
    /**
     * 判断字符串是否不为空
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
    
    /**
     * 判断字符串是否为空白（null、空字符串或只含空白字符）
     */
    public static boolean isBlank(String str) {
        return str == null || str.isBlank();
    }
    
    /**
     * 判断字符串是否不为空白
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }
    
    /**
     * 如果为空则返回默认值
     */
    public static String defaultIfEmpty(String str, String defaultValue) {
        return isEmpty(str) ? defaultValue : str;
    }
    
    /**
     * 如果为空白则返回默认值
     */
    public static String defaultIfBlank(String str, String defaultValue) {
        return isBlank(str) ? defaultValue : str;
    }
    
    // ==================== 截断与填充 ====================
    
    /**
     * 截取字符串（超出部分用省略号代替）
     */
    public static String truncate(String str, int maxLength) {
        return truncate(str, maxLength, "...");
    }
    
    /**
     * 截取字符串
     * @param str 原字符串
     * @param maxLength 最大长度（必须 >= 0）
     * @param suffix 超出时的后缀
     */
    public static String truncate(String str, int maxLength, String suffix) {
        if (maxLength < 0) {
            maxLength = 0;
        }
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        if (suffix == null) {
            suffix = "";
        }
        int targetLength = maxLength - suffix.length();
        if (targetLength <= 0) {
            // maxLength 太小，无法容纳 suffix，直接截断 suffix
            return suffix.length() <= maxLength ? suffix : suffix.substring(0, maxLength);
        }
        return str.substring(0, targetLength) + suffix;
    }
    
    /**
     * 左填充
     */
    public static String padLeft(String str, int length, char padChar) {
        if (str == null) {
            str = "";
        }
        if (str.length() >= length) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = str.length(); i < length; i++) {
            sb.append(padChar);
        }
        sb.append(str);
        return sb.toString();
    }
    
    /**
     * 右填充
     */
    public static String padRight(String str, int length, char padChar) {
        if (str == null) {
            str = "";
        }
        if (str.length() >= length) {
            return str;
        }
        StringBuilder sb = new StringBuilder(str);
        for (int i = str.length(); i < length; i++) {
            sb.append(padChar);
        }
        return sb.toString();
    }
    
    /**
     * 数字左补零
     */
    public static String padZero(int number, int length) {
        return padLeft(String.valueOf(number), length, '0');
    }
    
    // ==================== 格式验证 ====================
    
    /**
     * 判断是否是数字字符串
     */
    public static boolean isNumeric(String str) {
        if (isEmpty(str)) {
            return false;
        }
        return NUMERIC_PATTERN.matcher(str).matches();
    }
    
    /**
     * 判断是否是有效邮箱
     */
    public static boolean isEmail(String str) {
        if (isEmpty(str)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(str).matches();
    }
    
    /**
     * 判断是否是有效手机号（中国大陆）
     */
    public static boolean isPhone(String str) {
        if (isEmpty(str)) {
            return false;
        }
        return PHONE_PATTERN.matcher(str).matches();
    }
    
    // ==================== 游戏名称处理 ====================
    
    /**
     * 检查玩家名称是否合法
     * - 长度在指定范围内
     * - 不能为空白
     * - 可以包含中文、字母、数字
     */
    public static boolean isValidPlayerName(String name, int minLength, int maxLength) {
        if (isBlank(name)) {
            return false;
        }
        String trimmed = name.trim();
        int length = getDisplayLength(trimmed);
        return length >= minLength && length <= maxLength;
    }
    
    /**
     * 获取显示长度（中文算2，其他算1）
     */
    public static int getDisplayLength(String str) {
        if (isEmpty(str)) {
            return 0;
        }
        int length = 0;
        for (char c : str.toCharArray()) {
            if (isChinese(c)) {
                length += 2;
            } else {
                length += 1;
            }
        }
        return length;
    }
    
    /**
     * 判断字符是否是中文
     */
    public static boolean isChinese(char c) {
        return c >= '\u4e00' && c <= '\u9fff';
    }
    
    /**
     * 判断字符串是否包含中文
     */
    public static boolean containsChinese(String str) {
        if (isEmpty(str)) {
            return false;
        }
        for (char c : str.toCharArray()) {
            if (isChinese(c)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 敏感词过滤（替换为*）
     */
    public static String filterSensitive(String content, String[] sensitiveWords) {
        if (isEmpty(content) || sensitiveWords == null) {
            return content;
        }
        for (String word : sensitiveWords) {
            if (isNotEmpty(word)) {
                content = content.replace(word, "*".repeat(word.length()));
            }
        }
        return content;
    }
    
    /**
     * 检查是否包含敏感词
     */
    public static boolean containsSensitive(String content, String[] sensitiveWords) {
        if (isEmpty(content) || sensitiveWords == null) {
            return false;
        }
        for (String word : sensitiveWords) {
            if (isNotEmpty(word) && content.contains(word)) {
                return true;
            }
        }
        return false;
    }
    
    // ==================== 格式化 ====================
    
    /**
     * 格式化数字（千分位）
     */
    public static String formatNumber(long number) {
        return String.format("%,d", number);
    }
    
    /**
     * 格式化大数字（K、M、B）
     */
    public static String formatLargeNumber(long number) {
        if (number < 1000) {
            return String.valueOf(number);
        } else if (number < 1000000) {
            return String.format("%.1fK", number / 1000.0);
        } else if (number < 1000000000) {
            return String.format("%.1fM", number / 1000000.0);
        } else {
            return String.format("%.1fB", number / 1000000000.0);
        }
    }
    
    /**
     * 格式化百分比
     */
    public static String formatPercent(double value) {
        return String.format("%.1f%%", value * 100);
    }
    
    /**
     * 隐藏手机号中间4位
     */
    public static String hidePhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
    
    /**
     * 隐藏邮箱
     */
    public static String hideEmail(String email) {
        if (isEmpty(email)) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
    
    // ==================== 转换 ====================
    
    /**
     * 安全转换为 int
     */
    public static int toInt(String str) {
        return toInt(str, 0);
    }
    
    /**
     * 安全转换为 int（带默认值）
     */
    public static int toInt(String str, int defaultValue) {
        if (isEmpty(str)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 安全转换为 long
     */
    public static long toLong(String str) {
        return toLong(str, 0L);
    }
    
    /**
     * 安全转换为 long（带默认值）
     */
    public static long toLong(String str, long defaultValue) {
        if (isEmpty(str)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(str.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 安全转换为 double
     */
    public static double toDouble(String str) {
        return toDouble(str, 0.0);
    }
    
    /**
     * 安全转换为 double（带默认值）
     */
    public static double toDouble(String str, double defaultValue) {
        if (isEmpty(str)) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(str.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 安全转换为 boolean
     */
    public static boolean toBoolean(String str) {
        return toBoolean(str, false);
    }
    
    /**
     * 安全转换为 boolean（带默认值）
     */
    public static boolean toBoolean(String str, boolean defaultValue) {
        if (isEmpty(str)) {
            return defaultValue;
        }
        String s = str.trim().toLowerCase();
        if ("true".equals(s) || "1".equals(s) || "yes".equals(s)) {
            return true;
        }
        if ("false".equals(s) || "0".equals(s) || "no".equals(s)) {
            return false;
        }
        return defaultValue;
    }
    
    // ==================== 分割与连接 ====================
    
    /**
     * 分割字符串为 int 数组
     * 空字符串部分会被解析为 0
     */
    public static int[] splitToIntArray(String str, String separator) {
        if (isEmpty(str)) {
            return new int[0];
        }
        String[] parts = str.split(separator, -1);  // -1 保留尾部空字符串
        // 过滤掉空白部分
        List<Integer> list = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                list.add(toInt(trimmed));
            }
        }
        int[] result = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i);
        }
        return result;
    }
    
    /**
     * 分割字符串为 long 数组
     * 空字符串部分会被解析为 0
     */
    public static long[] splitToLongArray(String str, String separator) {
        if (isEmpty(str)) {
            return new long[0];
        }
        String[] parts = str.split(separator, -1);  // -1 保留尾部空字符串
        // 过滤掉空白部分
        List<Long> list = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                list.add(toLong(trimmed));
            }
        }
        long[] result = new long[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i);
        }
        return result;
    }
    
    /**
     * 连接 int 数组为字符串
     */
    public static String joinInts(int[] array, String separator) {
        if (array == null || array.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sb.append(separator);
            }
            sb.append(array[i]);
        }
        return sb.toString();
    }
    
    /**
     * 连接 long 数组为字符串
     */
    public static String joinLongs(long[] array, String separator) {
        if (array == null || array.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sb.append(separator);
            }
            sb.append(array[i]);
        }
        return sb.toString();
    }
}
