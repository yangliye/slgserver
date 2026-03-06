package com.muyi.config;

import com.muyi.common.util.time.TimeUtils;
import com.muyi.config.annotation.ConfigFile;
import com.muyi.config.loader.ConfigLoader;
import com.muyi.config.loader.XmlConfigLoader;
import com.muyi.config.reload.ConfigReloadListener;
import com.muyi.config.reload.ReloadResult;
import com.muyi.config.reload.ReloadResultBuilder;
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
 * 配置管理器（单例）
 * 全局共享的游戏配置中心
 *
 * @author muyi
 */
public class ConfigManager {

    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);

    private static final ConfigManager INSTANCE = new ConfigManager();

    /** 配置容器 */
    private final Map<Class<? extends IConfig>, ConfigContainer<?>> containers = new ConcurrentHashMap<>();

    /** 配置文件映射 */
    private final Map<Class<? extends IConfig>, String> configFiles = new ConcurrentHashMap<>();

    /** 配置加载器（线程安全） */
    private final List<ConfigLoader> loaders = new CopyOnWriteArrayList<>();

    /** 扩展名到加载器的缓存 */
    private final Map<String, ConfigLoader> loaderCache = new ConcurrentHashMap<>();

    /** 配置根目录 */
    private volatile String configRoot = "config";

    /** 是否已加载 */
    private volatile boolean loaded = false;

    /** 配置版本号（每次热更递增） */
    private final AtomicLong version = new AtomicLong(0);

    /** 热更监听器 */
    private final List<ConfigReloadListener> listeners = new CopyOnWriteArrayList<>();

    /** 读写锁，保证热更时的线程安全 */
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    /** 是否使用原子热更（失败回滚） */
    private volatile boolean atomicReload = true;

    private ConfigManager() {
        addLoader(new XmlConfigLoader());
    }

    public static ConfigManager getInstance() {
        return INSTANCE;
    }

    public ConfigManager setConfigRoot(String configRoot) {
        this.configRoot = configRoot;
        return this;
    }

    /**
     * 添加配置加载器
     */
    public ConfigManager addLoader(ConfigLoader loader) {
        loaders.add(loader);
        for (String ext : loader.supportedExtensions()) {
            loaderCache.put(ext, loader);
        }
        return this;
    }

    /**
     * 设置是否原子热更（默认开启）
     */
    public ConfigManager setAtomicReload(boolean atomic) {
        this.atomicReload = atomic;
        return this;
    }

    public ConfigManager addReloadListener(ConfigReloadListener listener) {
        listeners.add(listener);
        return this;
    }

    public ConfigManager removeReloadListener(ConfigReloadListener listener) {
        listeners.remove(listener);
        return this;
    }

    public long getVersion() {
        return version.get();
    }

    /**
     * 注册配置类型（手动注册）
     *
     * @param configClass 配置类
     * @param fileName    文件名（相对于 configRoot）
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
     * @param packageName 要扫描的包名
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
     * 扫描多个包
     */
    public ConfigManager scan(String... packageNames) {
        for (String packageName : packageNames) {
            scan(packageName);
        }
        return this;
    }

    /**
     * 根据类名生成文件名
     */
    private String generateFileName(Class<?> clazz) {
        String simpleName = clazz.getSimpleName();
        if (simpleName.endsWith("Config")) {
            simpleName = simpleName.substring(0, simpleName.length() - 6);
        }

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
     * 加载所有配置
     */
    public void loadAll() throws Exception {
        log.info("Loading all configs from: {}", configRoot);
        long startTime = TimeUtils.currentTimeMillis();

        for (Map.Entry<Class<? extends IConfig>, String> entry : configFiles.entrySet()) {
            loadConfig(entry.getKey(), entry.getValue());
        }

        loaded = true;
        long costTime = TimeUtils.currentTimeMillis() - startTime;
        log.info("All configs loaded, count={}, cost={}ms", containers.size(), costTime);
    }

    /**
     * 加载单个配置
     */
    @SuppressWarnings("unchecked")
    public <T extends IConfig> void loadConfig(Class<T> configClass, String fileName) throws Exception {
        String path = configRoot + File.separator + fileName;

        ConfigLoader loader = findLoader(fileName);
        if (loader == null) {
            throw new IllegalArgumentException("No loader found for: " + fileName);
        }

        List<T> configs = loader.load(path, configClass);

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

    @SuppressWarnings("unchecked")
    public ReloadResult reload(Class<? extends IConfig> configClass) {
        return reloadMultiple(configClass);
    }

    /**
     * 多表热更（核心方法）
     */
    @SuppressWarnings("unchecked")
    public ReloadResult reloadMultiple(Class<? extends IConfig>... configClasses) {
        if (configClasses == null || configClasses.length == 0) {
            return new ReloadResultBuilder()
                    .success(false)
                    .version(version.get())
                    .costTime(0)
                    .build();
        }

        long startTime = TimeUtils.currentTimeMillis();
        ReloadResultBuilder resultBuilder = new ReloadResultBuilder();

        notifyBeforeReload(configClasses);

        Map<Class<? extends IConfig>, ConfigContainer<?>> tempContainers = new ConcurrentHashMap<>();
        Map<Class<? extends IConfig>, ConfigContainer<?>> oldContainers = new ConcurrentHashMap<>();
        boolean allSuccess = true;

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

                List<IConfig> configs = (List<IConfig>) loader.load(path, configClass);
                ConfigContainer<IConfig> tempContainer = new ConfigContainer<>((Class<IConfig>) configClass);
                tempContainer.setAll(configs);
                tempContainers.put(configClass, tempContainer);

                ConfigContainer<?> oldContainer = containers.get(configClass);
                if (oldContainer != null) {
                    oldContainers.put(configClass, oldContainer);
                }

                resultBuilder.addSuccess(configClass.getSimpleName());

            } catch (Exception e) {
                log.error("Failed to load config: {}", configClass.getSimpleName(), e);
                resultBuilder.addFailed(configClass.getSimpleName(), e.getMessage());
                allSuccess = false;
            }
        }

        if (atomicReload && !allSuccess) {
            log.warn("Atomic reload failed, rolling back all changes");
            resultBuilder.success(false);
        } else {
            rwLock.writeLock().lock();
            try {
                for (Map.Entry<Class<? extends IConfig>, ConfigContainer<?>> entry : tempContainers.entrySet()) {
                    containers.put(entry.getKey(), entry.getValue());
                    notifyConfigReloaded(entry.getKey(), true);
                }

                long newVersion = version.incrementAndGet();
                resultBuilder.version(newVersion);

                log.info("Config reload completed, version={}, success={}, failed={}",
                        newVersion, tempContainers.size(),
                        configClasses.length - tempContainers.size());

            } finally {
                rwLock.writeLock().unlock();
            }
        }

        long costTime = TimeUtils.currentTimeMillis() - startTime;
        resultBuilder.costTime(costTime);

        ReloadResult result = resultBuilder.build();
        notifyAfterReload(result);

        return result;
    }

    /**
     * 按文件名热更配置
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

    @SuppressWarnings("unchecked")
    public <T extends IConfig> ConfigContainer<T> getContainer(Class<T> configClass) {
        rwLock.readLock().lock();
        try {
            return (ConfigContainer<T>) containers.get(configClass);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public Set<String> getRegisteredConfigNames() {
        Set<String> names = ConcurrentHashMap.newKeySet();
        for (Class<? extends IConfig> clazz : configFiles.keySet()) {
            names.add(clazz.getSimpleName());
        }
        return names;
    }

    public Class<? extends IConfig> findConfigClass(String simpleName) {
        for (Class<? extends IConfig> clazz : configFiles.keySet()) {
            if (clazz.getSimpleName().equals(simpleName)) {
                return clazz;
            }
        }
        return null;
    }

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

    public boolean isLoaded() {
        return loaded;
    }

    public int getRegisteredCount() {
        return configFiles.size();
    }

    private ConfigLoader findLoader(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0) {
            String ext = fileName.substring(dotIndex);
            ConfigLoader cached = loaderCache.get(ext);
            if (cached != null) {
                return cached;
            }
        }

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
     * 清空所有配置
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