package com.muyi.gameconfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * 配置容器
 * 存储一种类型的所有配置数据（仅按 ID 索引）
 * 
 * 注意：自定义索引（如按 type、level 分组）由业务模块自行维护，
 * 可在模块 start() 时构建，并监听 ConfigReloadListener 在热更时重建。
 *
 * @param <T> 配置类型
 * @author muyi
 */
public class ConfigContainer<T extends IConfig> {
    
    /** 配置类型 */
    private final Class<T> configClass;
    
    /** ID -> 配置 */
    private final Map<Integer, T> configMap = new ConcurrentHashMap<>();
    
    /** 有序列表（按文档顺序） */
    private volatile List<T> configList = new ArrayList<>();
    
    public ConfigContainer(Class<T> configClass) {
        this.configClass = configClass;
    }
    
    /**
     * 添加配置
     */
    public void put(T config) {
        configMap.put(config.getId(), config);
    }
    
    /**
     * 批量设置配置（原子替换全部）
     */
    public void setAll(Collection<T> configs) {
        // 直接构建有序列表（保持原始文档顺序）
        List<T> newList = new ArrayList<>(configs);
        
        // 构建 ID 映射
        Map<Integer, T> newMap = new ConcurrentHashMap<>(configs.size());
        for (T config : configs) {
            newMap.put(config.getId(), config);
        }
        
        // 原子替换
        this.configList = Collections.unmodifiableList(newList);
        this.configMap.clear();
        this.configMap.putAll(newMap);
    }
    
    /**
     * 根据 ID 获取配置
     */
    public T get(int id) {
        return configMap.get(id);
    }
    
    /**
     * 根据 ID 获取配置（不存在则抛异常）
     */
    public T getOrThrow(int id) {
        T config = configMap.get(id);
        if (config == null) {
            throw new IllegalArgumentException(
                    "Config not found: " + configClass.getSimpleName() + ", id=" + id);
        }
        return config;
    }
    
    /**
     * 获取所有配置（有序）
     */
    public List<T> getAll() {
        return configList;
    }
    
    /**
     * 根据条件查找
     */
    public List<T> findAll(Predicate<T> predicate) {
        List<T> result = new ArrayList<>();
        for (T config : configList) {
            if (predicate.test(config)) {
                result.add(config);
            }
        }
        return result;
    }
    
    /**
     * 根据条件查找第一个
     */
    public T findFirst(Predicate<T> predicate) {
        for (T config : configList) {
            if (predicate.test(config)) {
                return config;
            }
        }
        return null;
    }
    
    /**
     * 是否存在
     */
    public boolean contains(int id) {
        return configMap.containsKey(id);
    }
    
    /**
     * 获取配置数量
     */
    public int size() {
        return configMap.size();
    }
    
    /**
     * 清空
     */
    public void clear() {
        configMap.clear();
        configList = new ArrayList<>();
    }
    
    /**
     * 获取配置类型
     */
    public Class<T> getConfigClass() {
        return configClass;
    }
}
