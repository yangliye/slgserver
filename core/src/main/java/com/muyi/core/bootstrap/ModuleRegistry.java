package com.muyi.core.bootstrap;

import com.muyi.core.module.GameModule;

import java.util.Collection;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模块注册中心
 * 管理所有游戏模块的注册和查找
 *
 * @author muyi
 */
public class ModuleRegistry {
    
    private static final ModuleRegistry INSTANCE = new ModuleRegistry();
    
    /** 已注册的模块 */
    private final Map<String, GameModule> modules = new ConcurrentHashMap<>();
    
    private ModuleRegistry() {
    }
    
    public static ModuleRegistry getInstance() {
        return INSTANCE;
    }
    
    /**
     * 注册模块（原子操作，防止重复注册）
     */
    public void register(GameModule module) {
        GameModule existing = modules.putIfAbsent(module.name(), module);
        if (existing != null) {
            throw new IllegalStateException("Module already registered: " + module.name());
        }
    }
    
    /**
     * 获取模块
     */
    public GameModule get(String name) {
        return modules.get(name);
    }
    
    /**
     * 获取所有模块
     */
    public Collection<GameModule> getAll() {
        return modules.values();
    }
    
    /**
     * 是否包含模块
     */
    public boolean contains(String name) {
        return modules.containsKey(name);
    }
    
    /**
     * 移除模块
     */
    public GameModule remove(String name) {
        return modules.remove(name);
    }
    
    /**
     * 清空所有模块
     */
    public void clear() {
        modules.clear();
    }
    
    /**
     * 通过 SPI 自动发现并注册模块
     */
    public void discoverModules() {
        ServiceLoader<GameModule> loader = ServiceLoader.load(GameModule.class);
        for (GameModule module : loader) {
            if (!contains(module.name())) {
                register(module);
            }
        }
    }
}
