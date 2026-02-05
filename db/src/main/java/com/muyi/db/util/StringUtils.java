package com.muyi.db.util;

/**
 * 字符串工具类
 */
public final class StringUtils {

    private StringUtils() {
        // 工具类禁止实例化
    }

    /**
     * 驼峰命名转下划线命名
     * <p>
     * 示例：
     * <ul>
     *   <li>userName -> user_name</li>
     *   <li>UserName -> user_name</li>
     *   <li>userID -> user_i_d</li>
     * </ul>
     *
     * @param camelCase 驼峰命名字符串
     * @return 下划线命名字符串
     */
    public static String camelToSnake(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * 检查字符串是否为空或空白
     *
     * @param str 字符串
     * @return 是否为空或空白
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 检查字符串是否不为空且不为空白
     *
     * @param str 字符串
     * @return 是否不为空且不为空白
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }
}
