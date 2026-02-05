package com.muyi.common.util.codec;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.TypeReference;

import java.util.List;
import java.util.Map;

/**
 * JSON 工具类
 * 基于 FastJSON2 实现
 */
public final class JsonUtils {
    
    private JsonUtils() {
        // 工具类禁止实例化
    }
    
    // ==================== 序列化 ====================
    
    /**
     * 对象转 JSON 字符串
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        return JSON.toJSONString(obj);
    }
    
    /**
     * 对象转 JSON 字符串（格式化输出）
     */
    public static String toJsonPretty(Object obj) {
        if (obj == null) {
            return null;
        }
        return JSON.toJSONString(obj, JSONWriter.Feature.PrettyFormat);
    }
    
    /**
     * 对象转 JSON 字节数组
     */
    public static byte[] toJsonBytes(Object obj) {
        if (obj == null) {
            return new byte[0];
        }
        return JSON.toJSONBytes(obj);
    }
    
    // ==================== 反序列化 ====================
    
    /**
     * JSON 字符串转对象
     */
    public static <T> T parse(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return JSON.parseObject(json, clazz);
    }
    
    /**
     * JSON 字节数组转对象
     */
    public static <T> T parse(byte[] jsonBytes, Class<T> clazz) {
        if (jsonBytes == null || jsonBytes.length == 0) {
            return null;
        }
        return JSON.parseObject(jsonBytes, clazz);
    }
    
    /**
     * JSON 字符串转泛型对象
     * 用于复杂泛型类型，如 List<User>、Map<String, User> 等
     * 
     * 使用示例:
     * List<User> users = JsonUtils.parse(json, new TypeReference<List<User>>(){});
     */
    public static <T> T parse(String json, TypeReference<T> typeReference) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return JSON.parseObject(json, typeReference);
    }
    
    /**
     * JSON 字符串转 List
     * @return List 对象，如果输入为空则返回空列表
     */
    public static <T> List<T> parseList(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        List<T> result = JSON.parseArray(json, clazz);
        return result != null ? result : new java.util.ArrayList<>();
    }
    
    /**
     * JSON 字符串转 JSONObject
     */
    public static JSONObject parseObject(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return JSON.parseObject(json);
    }
    
    /**
     * JSON 字符串转 JSONArray
     */
    public static JSONArray parseArray(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return JSON.parseArray(json);
    }
    
    /**
     * JSON 字符串转 Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseMap(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return JSON.parseObject(json, Map.class);
    }
    
    // ==================== 对象转换 ====================
    
    /**
     * 对象转换为另一个类型
     * 通过 JSON 序列化/反序列化实现深拷贝转换
     */
    public static <T> T convert(Object obj, Class<T> clazz) {
        if (obj == null) {
            return null;
        }
        return JSON.parseObject(JSON.toJSONString(obj), clazz);
    }
    
    /**
     * 对象深拷贝
     */
    @SuppressWarnings("unchecked")
    public static <T> T deepCopy(T obj) {
        if (obj == null) {
            return null;
        }
        return (T) JSON.parseObject(JSON.toJSONString(obj), obj.getClass());
    }
    
    // ==================== 判断方法 ====================
    
    /**
     * 判断字符串是否是有效的 JSON
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }
        try {
            JSON.parse(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 判断字符串是否是 JSON 对象
     */
    public static boolean isJsonObject(String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }
        json = json.trim();
        return json.startsWith("{") && json.endsWith("}");
    }
    
    /**
     * 判断字符串是否是 JSON 数组
     */
    public static boolean isJsonArray(String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }
        json = json.trim();
        return json.startsWith("[") && json.endsWith("]");
    }
    
    // ==================== JSONObject 便捷方法 ====================
    
    /**
     * 从 JSONObject 安全获取字符串
     */
    public static String getString(JSONObject json, String key) {
        return getString(json, key, null);
    }
    
    /**
     * 从 JSONObject 安全获取字符串（带默认值）
     */
    public static String getString(JSONObject json, String key, String defaultValue) {
        if (json == null || !json.containsKey(key)) {
            return defaultValue;
        }
        return json.getString(key);
    }
    
    /**
     * 从 JSONObject 安全获取整数
     */
    public static int getInt(JSONObject json, String key) {
        return getInt(json, key, 0);
    }
    
    /**
     * 从 JSONObject 安全获取整数（带默认值）
     */
    public static int getInt(JSONObject json, String key, int defaultValue) {
        if (json == null || !json.containsKey(key)) {
            return defaultValue;
        }
        return json.getIntValue(key);
    }
    
    /**
     * 从 JSONObject 安全获取长整数
     */
    public static long getLong(JSONObject json, String key) {
        return getLong(json, key, 0L);
    }
    
    /**
     * 从 JSONObject 安全获取长整数（带默认值）
     */
    public static long getLong(JSONObject json, String key, long defaultValue) {
        if (json == null || !json.containsKey(key)) {
            return defaultValue;
        }
        return json.getLongValue(key);
    }
    
    /**
     * 从 JSONObject 安全获取布尔值
     */
    public static boolean getBoolean(JSONObject json, String key) {
        return getBoolean(json, key, false);
    }
    
    /**
     * 从 JSONObject 安全获取布尔值（带默认值）
     */
    public static boolean getBoolean(JSONObject json, String key, boolean defaultValue) {
        if (json == null || !json.containsKey(key)) {
            return defaultValue;
        }
        return json.getBooleanValue(key);
    }
}
