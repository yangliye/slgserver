package com.muyi.config;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 配置管理器（单例�?
 * 全局共享的游戏配置中�?
 * 
 * 使用示例�?
 * <pre>{@code
 * // 1. 注册配置类型
 * ConfigManager.getInstance().register(UnitConfig.class, "config/unit.xml");
 * 
 * // 2. 加载所有配�?
 * ConfigManager.getInstance().loadAll();
 * 
 * // 3. 获取配置
 * UnitConfig unit = ConfigManager.getInstance().get(UnitConfig.class, 1001);
 * List<UnitConfig> all = ConfigManager.getInstance().getAll(UnitConfig.class);
 * }</pre>
 *
 * @author muyi
 */
public class ConfigManager {
    
    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);
    
    /** 单例 */
    private static final ConfigManager INSTANCE = new ConfigManager();
    
    /** 配置容器 Class -> ConfigContainer */
    private final Map<Class<? extends IConfig>, ConfigContainer<?>> containers = new ConcurrentHashMap<>();
    
    /** 配置文件映射 Class -> filePath */
    private final Map<Class<? extends IConfig>, String> configFiles = new ConcurrentHashMap<>();
    
    /** 配置加载器（线程安全�?*/
    private final List<ConfigLoader> loaders = new CopyOnWriteArrayList<>();
    
    /** 扩展�?-> 加载器缓�?*/
    private final Map<String, ConfigLoader> loaderCache = new ConcurrentHashMap<>();
    
    /** 配置根目�?*/
    private volatile String configRoot = "config";
    
    /** 是否已加�?*/
    private volatile boolean loaded = false;
    
    /** 配置版本号（每次热更递增�?*/
    private final AtomicLong version = new AtomicLong(0);
    
    /** 热更监听�?*/
    private final List<ConfigReloadListener> listeners = new CopyOnWriteArrayList<>();
    
    /** 读写锁，保证热更时的线程安全 */
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    
    /** 是否使用原子热更（失败回滚） */
    private volatile boolean atomicReload = true;
    
    private ConfigManager() {
        // 注册默认加载�?
        addLoader(new XmlConfigLoader());
    }
    
    /**
     * 获取单例实例
     */
    public static ConfigManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 设置配置根目�?
     */
    public ConfigManager setConfigRoot(String configRoot) {
        this.configRoot = configRoot;
        return this;
    }
    
    /**
     * 添加配置加载�?
     */
    public ConfigManager addLoader(ConfigLoader loader) {
        loaders.add(loader);
        // 更新缓存
        for (String ext : loader.supportedExtensions()) {
            loaderCache.put(ext, loader);
        }
        return this;
    }
    
    /**
     * 设置是否原子热更（默认开启）
     * 开启后，多表热更时任一表失败则全部回滚
     */
    public ConfigManager setAtomicReload(boolean atomic) {
        this.atomicReload = atomic;
        return this;
    }
    
    /**
     * 添加热更监听�?
     */
    public ConfigManager addReloadListener(ConfigReloadListener listener) {
        listeners.add(listener);
        return this;
    }
    
    /**
     * 移除热更监听�?
     */
    public ConfigManager removeReloadListener(ConfigReloadListener listener) {
        listeners.remove(listener);
        return this;
    }
    
    /**
     * 获取当前版本�?
     */
    public long getVersion() {
        return version.get();
    }
    
    /**
     * 注册配置类型（手动注册）
     *
     * @param configClass 配置�?
     * @param fileName 文件名（相对�?configRoot�?
     */
    public <T extends IConfig> ConfigManager register(Class<T> configClass, String fileName) {
        containers.put(configClass, new ConfigContainer<>(configClass));
        configFiles.put(configClass, fileName);
        log.debug("Registered config: {} -> {}", configClass.getSimpleName(), fileName);
        return this;
    }
    
    /**
     * 自动扫描并注册带 @ConfigFile 注解的配置类
     * 
     * @param packageName 要扫描的包名，如 "com.muyi.game.config"
     */
    @SuppressWarnings("unchecked")
    public ConfigManager scan(String packageName) {
        log.info("Scanning config classes in package: {}", packageName);
        
        try (ScanResult scanResult = new ClassGraph()
                .enableAnnotationInfo()
                .enableClassInfo()
                .acceptPackages(packageName)
                .scan()) {
            
            for (ClassInfo classInfo : scanResult.getClassesWithAnnotation(ConfigFile.class)) {
                Class<?> clazz = classInfo.loadClass();
                
                if (!IConfig.class.isAssignableFrom(clazz)) {
                    log.warn("Class {} has @ConfigFile but does not implement IConfig, skipped", clazz.getName());
                    continue;
                }
                
                ConfigFile annotation = clazz.getAnnotation(ConfigFile.class);
                String fileName = annotation.value();
                
                // 如果未指定文件名，按类名生成
                if (fileName == null || fileName.isEmpty()) {
                    fileName = generateFileName(clazz);
                }
                
                register((Class<? extends IConfig>) clazz, fileName);
            }
        }
        
        log.info("Scan completed, found {} config classes", configFiles.size());
        return this;
    }
    
    /**
     * 扫描多个�?
     */
    public ConfigManager scan(String... packageNames) {
        for (String packageName : packageNames) {
            scan(packageName);
        }
        return this;
    }
    
    /**
     * 根据类名生成文件�?
     * UnitConfig -> unit.xml
     * BuildingConfig -> building.xml
     */
    private String generateFileName(Class<?> clazz) {
        String simpleName = clazz.getSimpleName();
        
        // 移除 Config 后缀
        if (simpleName.endsWith("Config")) {
            simpleName = simpleName.substring(0, simpleName.length() - 6);
        }
        
        // 驼峰转下划线，再转小�?
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < simpleName.length(); i++) {
            char c = simpleName.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append('_');
            }
            sb.append(Character.toLowerCase(c));
        }
        
        return sb.toString() + ".xml";
    }
    
    /**
     * 加载所有配�?
     */
    public void loadAll() throws Exception {
        log.info("Loading all configs from: {}", configRoot);
        long startTime = System.currentTimeMillis();
        
        for (Map.Entry<Class<? extends IConfig>, String> entry : configFiles.entrySet()) {
            loadConfig(entry.getKey(), entry.getValue());
        }
        
        loaded = true;
        long costTime = System.currentTimeMillis() - startTime;
        log.info("All configs loaded, count={}, cost={}ms", containers.size(), costTime);
    }
    
    /**
     * 加载单个配置
     */
    @SuppressWarnings("unchecked")
    public <T extends IConfig> void loadConfig(Class<T> configClass, String fileName) throws Exception {
        String path = configRoot + File.separator + fileName;
        
        // 查找合适的加载�?
        ConfigLoader loader = findLoader(fileName);
        if (loader == null) {
            throw new IllegalArgumentException("No loader found for: " + fileName);
        }
        
        // 加载配置
        List<T> configs = loader.load(path, configClass);
        
        // 存入容器
        ConfigContainer<T> container = (ConfigContainer<T>) containers.get(configClass);
        if (container == null) {
            container = new ConfigContainer<>(configClass);
            containers.put(configClass, container);
        }
        container.setAll(configs);
        
        log.info("Loaded config: {} ({} items)", configClass.getSimpleName(), configs.size());
    }
    
    /**
     * 重新加载所有配置（热更新）
     */
    public ReloadResult reloadAll() {
        log.info("Reloading all configs...");
        @SuppressWarnings("unchecked")
        Class<? extends IConfig>[] allClasses = configFiles.keySet().toArray(new Class[0]);
        return reloadMultiple(allClasses);
    }
    
    /**
     * 重新加载单个配置
     */
    @SuppressWarnings("unchecked")
    public ReloadResult reload(Class<? extends IConfig> configClass) {
        return reloadMultiple(configClass);
    }
    
    /**
     * 多表热更（核心方法）
     * 支持原子性更新，任一表失败可回滚
     * 
     * @param configClasses 要热更的配置�?
     * @return 热更结果
     */
    @SuppressWarnings("unchecked")
    public ReloadResult reloadMultiple(Class<? extends IConfig>... configClasses) {
        if (configClasses == null || configClasses.length == 0) {
            return new ReloadResult.Builder()
                    .success(false)
                    .version(version.get())
                    .costTime(0)
                    .build();
        }
        
        long startTime = System.currentTimeMillis();
        ReloadResult.Builder resultBuilder = new ReloadResult.Builder();
        
        // 通知监听器：热更开�?
        notifyBeforeReload(configClasses);
        
        // 临时容器（双缓冲�?
        Map<Class<? extends IConfig>, ConfigContainer<?>> tempContainers = new ConcurrentHashMap<>();
        Map<Class<? extends IConfig>, ConfigContainer<?>> oldContainers = new ConcurrentHashMap<>();
        
        boolean allSuccess = true;
        
        // 第一阶段：加载所有配置到临时容器
        for (Class<? extends IConfig> configClass : configClasses) {
            String fileName = configFiles.get(configClass);
            if (fileName == null) {
                resultBuilder.addFailed(configClass.getSimpleName(), "Config not registered");
                allSuccess = false;
                continue;
            }
            
            try {
                String path = configRoot + File.separator + fileName;
                ConfigLoader loader = findLoader(fileName);
                if (loader == null) {
                    resultBuilder.addFailed(configClass.getSimpleName(), "No loader found");
                    allSuccess = false;
                    continue;
                }
                
                // 加载到临时容�?
                List<IConfig> configs = (List<IConfig>) loader.load(path, configClass);
                ConfigContainer<IConfig> tempContainer = new ConfigContainer<>((Class<IConfig>) configClass);
                tempContainer.setAll(configs);
                
                tempContainers.put(configClass, tempContainer);
                
                // 保存旧容器（用于回滚�?
                ConfigContainer<?> oldContainer = containers.get(configClass);
                if (oldContainer != null) {
                    oldContainers.put(configClass, oldContainer);
                }
                
                resultBuilder.addSuccess(configClass.getSimpleName());
                log.debug("Loaded {} to temp container ({} items)", configClass.getSimpleName(), configs.size());
                
            } catch (Exception e) {
                log.error("Failed to load config: {}", configClass.getSimpleName(), e);
                resultBuilder.addFailed(configClass.getSimpleName(), e.getMessage());
                allSuccess = false;
            }
        }
        
        // 第二阶段：原子切换或部分更新
        if (atomicReload && !allSuccess) {
            // 原子模式：有失败则全部回�?
            log.warn("Atomic reload failed, rolling back all changes");
            resultBuilder.success(false);
        } else {
            // 切换成功的配�?
            rwLock.writeLock().lock();
            try {
                for (Map.Entry<Class<? extends IConfig>, ConfigContainer<?>> entry : tempContainers.entrySet()) {
                    containers.put(entry.getKey(), entry.getValue());
                    
                    // 通知单个配置热更完成
                    notifyConfigReloaded(entry.getKey(), true);
                }
                
                // 更新版本�?
                long newVersion = version.incrementAndGet();
                resultBuilder.version(newVersion);
                
                log.info("Config reload completed, version={}, success={}, failed={}", 
                        newVersion, tempContainers.size(), 
                        configClasses.length - tempContainers.size());
                
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        
        long costTime = System.currentTimeMillis() - startTime;
        resultBuilder.costTime(costTime);
        
        ReloadResult result = resultBuilder.build();
        
        // 通知监听器：热更完成
        notifyAfterReload(result);
        
        return result;
    }
    
    /**
     * 按文件名热更配置
     * 
     * @param fileNames 配置文件名列�?
     */
    public ReloadResult reloadByFileNames(String... fileNames) {
        List<Class<? extends IConfig>> classesToReload = new ArrayList<>();
        
        for (String fileName : fileNames) {
            for (Map.Entry<Class<? extends IConfig>, String> entry : configFiles.entrySet()) {
                if (entry.getValue().equals(fileName)) {
                    classesToReload.add(entry.getKey());
                    break;
                }
            }
        }
        
        @SuppressWarnings("unchecked")
        Class<? extends IConfig>[] classes = classesToReload.toArray(new Class[0]);
        return reloadMultiple(classes);
    }
    
    // ========== 监听器通知方法 ==========
    
    private void notifyBeforeReload(Class<? extends IConfig>[] configClasses) {
        for (ConfigReloadListener listener : listeners) {
            try {
                listener.beforeReload(configClasses);
            } catch (Exception e) {
                log.error("Listener beforeReload error", e);
            }
        }
    }
    
    private void notifyAfterReload(ReloadResult result) {
        for (ConfigReloadListener listener : listeners) {
            try {
                listener.afterReload(result);
            } catch (Exception e) {
                log.error("Listener afterReload error", e);
            }
        }
    }
    
    private void notifyConfigReloaded(Class<? extends IConfig> configClass, boolean success) {
        for (ConfigReloadListener listener : listeners) {
            try {
                listener.onConfigReloaded(configClass, success);
            } catch (Exception e) {
                log.error("Listener onConfigReloaded error", e);
            }
        }
    }
    
    /**
     * 根据 ID 获取配置
     */
    @SuppressWarnings("unchecked")
    public <T extends IConfig> T get(Class<T> configClass, int id) {
        rwLock.readLock().lock();
        try {
            ConfigContainer<T> container = (ConfigContainer<T>) containers.get(configClass);
            return container != null ? container.get(id) : null;
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    /**
     * 根据 ID 获取配置（不存在则抛异常�?
     */
    @SuppressWarnings("unchecked")
    public <T extends IConfig> T getOrThrow(Class<T> configClass, int id) {
        rwLock.readLock().lock();
        try {
            ConfigContainer<T> container = (ConfigContainer<T>) containers.get(configClass);
            if (container == null) {
                throw new IllegalArgumentException("Config not registered: " + configClass.getSimpleName());
            }
            return container.getOrThrow(id);
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    /**
     * 获取所有配�?
     */
    @SuppressWarnings("unchecked")
    public <T extends IConfig> List<T> getAll(Class<T> configClass) {
        rwLock.readLock().lock();
        try {
            ConfigContainer<T> container = (ConfigContainer<T>) containers.get(configClass);
            return container != null ? container.getAll() : Collections.emptyList();
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    /**
     * 获取配置容器
     */
    @SuppressWarnings("unchecked")
    public <T extends IConfig> ConfigContainer<T> getContainer(Class<T> configClass) {
        rwLock.readLock().lock();
        try {
            return (ConfigContainer<T>) containers.get(configClass);
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    /**
     * 获取所有已注册的配置类�?
     */
    public Set<String> getRegisteredConfigNames() {
        Set<String> names = ConcurrentHashMap.newKeySet();
        for (Class<? extends IConfig> clazz : configFiles.keySet()) {
            names.add(clazz.getSimpleName());
        }
        return names;
    }
    
    /**
     * 根据类名查找配置�?
     */
    public Class<? extends IConfig> findConfigClass(String simpleName) {
        for (Class<? extends IConfig> clazz : configFiles.keySet()) {
            if (clazz.getSimpleName().equals(simpleName)) {
                return clazz;
            }
        }
        return null;
    }
    
    /**
     * 根据类名热更配置
     */
    public ReloadResult reloadByNames(String... configNames) {
        List<Class<? extends IConfig>> classes = new ArrayList<>();
        for (String name : configNames) {
            Class<? extends IConfig> clazz = findConfigClass(name);
            if (clazz != null) {
                classes.add(clazz);
            } else {
                log.warn("Config class not found: {}", name);
            }
        }
        
        @SuppressWarnings("unchecked")
        Class<? extends IConfig>[] arr = classes.toArray(new Class[0]);
        return reloadMultiple(arr);
    }
    
    /**
     * 是否已加�?
     */
    public boolean isLoaded() {
        return loaded;
    }
    
    /**
     * 获取已注册的配置数量
     */
    public int getRegisteredCount() {
        return configFiles.size();
    }
    
    /**
     * 查找合适的加载器（使用缓存�?
     */
    private ConfigLoader findLoader(String fileName) {
        // 从缓存中查找
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0) {
            String ext = fileName.substring(dotIndex);
            ConfigLoader cached = loaderCache.get(ext);
            if (cached != null) {
                return cached;
            }
        }
        
        // 回退到遍历查�?
        for (ConfigLoader loader : loaders) {
            for (String ext : loader.supportedExtensions()) {
                if (fileName.endsWith(ext)) {
                    return loader;
                }
            }
        }
        return null;
    }
    
    /**
     * 清空所有配�?
     */
    public void clear() {
        rwLock.writeLock().lock();
        try {
            for (ConfigContainer<?> container : containers.values()) {
                container.clear();
            }
            loaded = false;
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
