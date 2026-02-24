package com.muyi.rpc.registry;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.muyi.common.util.time.TimeUtils;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson2.JSON;

/**
 * 基于ZooKeeper的服务注册中心
 * 使用Curator框架实现服务注册与发现
 * 专为大型游戏服务器多工程之间的RPC调用设计
 * 
 * ZooKeeper节点结构：
 * /rpc
 *   /{serviceKey}  (临时节点，内容为ServiceInstance JSON，一个serviceKey对应唯一地址)
 * 
 * serviceKey格式：interfaceName#serverId
 * 示例：
 *   /rpc/com.example.IGameService_1  -> {"address":"192.168.1.10:8001",...}
 *   /rpc/com.example.IGameService_2  -> {"address":"192.168.1.11:8002",...}
 *   /rpc/com.example.IGateService_1  -> {"address":"192.168.1.20:9001",...}
 *
 * 特性：
 * - 服务自动注册与发现
 * - 一个 serviceKey 对应唯一地址（serverId 唯一标识服务实例）
 * - 服务权重支持
 * - 服务元数据（区服ID、负载信息等）
 * - 优雅下线
 * - 自动重连与重新注册
 * - 服务变更实时监听
 *
 * @author muyi
 */
public class ZookeeperServiceRegistry implements ServiceRegistry, ServiceDiscovery {
    
    private static final Logger logger = LoggerFactory.getLogger(ZookeeperServiceRegistry.class);
    
    /** ZooKeeper根路径 */
    private static final String ROOT_PATH = "/rpc";
    
    /** Curator客户端 */
    private final CuratorFramework client;
    
    /** 本地服务地址缓存: serviceKey -> address */
    private final Map<String, String> serviceCache = new ConcurrentHashMap<>();
    
    /** 本地服务实例缓存（包含元数据） */
    private final Map<String, Map<String, ServiceInstance>> instanceCache = new ConcurrentHashMap<>();
    
    /** 已注册的服务（用于重连后重新注册） */
    private final Map<String, ServiceInstance> registeredServices = new ConcurrentHashMap<>();
    
    /** 服务监听缓存 */
    private final Map<String, CuratorCache> watcherCache = new ConcurrentHashMap<>();
    
    /** 服务变更监听器 */
    private final Map<String, List<Consumer<List<String>>>> listeners = new ConcurrentHashMap<>();
    
    /** 服务实例变更监听器 */
    private final Map<String, List<Consumer<List<ServiceInstance>>>> instanceListeners = new ConcurrentHashMap<>();
    
    /** 服务下线回调（用于通知 RpcProxyManager 清理缓存，参数为 serviceKey） */
    private Consumer<String> serviceOfflineCallback;
    
    /** 调度器（用于优雅下线等延迟任务） */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual().name("zk-scheduler").factory()
    );
    
    /** 服务权重 */
    private int weight = 100;
    
    /** 服务器ID */
    private String serverId;
    
    /** 扩展元数据 */
    private final Map<String, String> metadata = new ConcurrentHashMap<>();
    
    /**
     * 创建ZooKeeper注册中心
     * 
     * @param connectString ZooKeeper连接地址，如 "127.0.0.1:2181" 或 "host1:2181,host2:2181"
     */
    public ZookeeperServiceRegistry(String connectString) {
        this(connectString, 5000, 3000, "");
    }
    
    /**
     * 创建ZooKeeper注册中心
     * 
     * @param connectString     ZooKeeper连接地址
     * @param sessionTimeout    会话超时时间（毫秒）
     * @param connectionTimeout 连接超时时间（毫秒）
     * @param namespace         命名空间（可选，用于环境隔离）
     */
    public ZookeeperServiceRegistry(String connectString, int sessionTimeout, 
                                     int connectionTimeout, String namespace) {
        this(connectString, sessionTimeout, connectionTimeout, namespace, 1000, 3, 5000);
    }
    
    /**
     * 创建ZooKeeper注册中心（完整参数）
     *
     * @param connectString       ZooKeeper连接地址
     * @param sessionTimeout      会话超时时间（毫秒）
     * @param connectionTimeout   连接超时时间（毫秒）
     * @param namespace           命名空间（可选，用于环境隔离）
     * @param retryInitialDelay   重试初始延迟（毫秒）
     * @param retryMaxRetries     最大重试次数
     * @param retryMaxDelay       重试最大延迟（毫秒）
     */
    public ZookeeperServiceRegistry(String connectString, int sessionTimeout, 
                                     int connectionTimeout, String namespace,
                                     int retryInitialDelay, int retryMaxRetries, int retryMaxDelay) {
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .connectString(connectString)
                .sessionTimeoutMs(sessionTimeout)
                .connectionTimeoutMs(connectionTimeout)
                .retryPolicy(new ExponentialBackoffRetry(retryInitialDelay, retryMaxRetries, retryMaxDelay));
        
        if (namespace != null && !namespace.isEmpty()) {
            builder.namespace(namespace);
        }
        
        this.client = builder.build();
        
        // 生成默认服务器ID
        try {
            this.serverId = InetAddress.getLocalHost().getHostName() + "-" + TimeUtils.currentTimeMillis();
        } catch (Exception e) {
            this.serverId = "server-" + TimeUtils.currentTimeMillis();
        }
        
        // 添加连接状态监听
        client.getConnectionStateListenable().addListener((curatorFramework, state) -> {
            logger.info("ZooKeeper connection state changed: {}", state);
            if (state == ConnectionState.RECONNECTED) {
                // 重连后重新注册服务
                reRegisterServices();
            }
        });
        
        // 启动客户端
        client.start();
        
        try {
            // 等待连接建立
            boolean connected = client.blockUntilConnected(connectionTimeout, TimeUnit.MILLISECONDS);
            if (!connected) {
                throw new RuntimeException("Failed to connect to ZooKeeper: " + connectString);
            }
            logger.info("Connected to ZooKeeper: {}", connectString);
            
            // 确保根路径存在
            ensurePath(ROOT_PATH);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ZooKeeper registry", e);
        }
    }
    
    @Override
    public void register(String serviceKey, String address) {
        try {
            // 创建服务实例
            ServiceInstance instance = new ServiceInstance(serviceKey, address);
            instance.setServerId(serverId);
            instance.setWeight(weight);
            instance.getMetadata().putAll(metadata);
            
            // 注册服务实例
            registerInstance(instance);
            
        } catch (Exception e) {
            logger.error("Failed to register service to ZooKeeper", e);
            throw new RuntimeException("Failed to register service", e);
        }
    }
    
    /**
     * 注册服务实例（带完整元数据）
     * 一个 serviceKey 只对应一个唯一地址
     */
    public void registerInstance(ServiceInstance instance) {
        try {
            String serviceKey = instance.getServiceKey();
            String address = instance.getAddress();
            
            // 服务节点路径（直接在 serviceKey 节点存储数据）
            String servicePath = buildServicePath(serviceKey);
            
            // 序列化服务实例
            String instanceData = JSON.toJSONString(instance);
            
            // 创建或更新临时节点
            if (client.checkExists().forPath(servicePath) != null) {
                // 节点已存在，更新数据
                client.setData().forPath(servicePath, instanceData.getBytes(StandardCharsets.UTF_8));
            } else {
                // 确保父路径存在
                ensurePath(ROOT_PATH);
                // 创建临时节点（服务下线后自动删除）
                client.create()
                        .withMode(CreateMode.EPHEMERAL)
                        .forPath(servicePath, instanceData.getBytes(StandardCharsets.UTF_8));
            }
            
            // 记录已注册的服务
            registeredServices.put(serviceKey, instance);
            
            logger.info("Register service to ZooKeeper: {} -> {} (weight={})", 
                    serviceKey, address, instance.getWeight());
        } catch (Exception e) {
            logger.error("Failed to register service instance to ZooKeeper", e);
            throw new RuntimeException("Failed to register service", e);
        }
    }
    
    @Override
    public void unregister(String serviceKey, String address) {
        try {
            String servicePath = buildServicePath(serviceKey);
            
            if (client.checkExists().forPath(servicePath) != null) {
                client.delete().forPath(servicePath);
            }
            
            registeredServices.remove(serviceKey);
            serviceCache.remove(serviceKey);
            instanceCache.remove(serviceKey);
            
            logger.info("Unregister service from ZooKeeper: {} -> {}", serviceKey, address);
        } catch (Exception e) {
            logger.error("Failed to unregister service from ZooKeeper", e);
        }
    }
    
    /**
     * 优雅下线（先标记为DRAINING状态，等待一段时间后再注销）
     */
    public void gracefulUnregister(String serviceKey, String address, long drainTimeMs) {
        try {
            ServiceInstance instance = registeredServices.get(serviceKey);
            if (instance != null) {
                // 更新状态为DRAINING
                instance.setStatus(ServiceStatus.DRAINING);
                registerInstance(instance);
                
                logger.info("Service entering drain mode: {} -> {}", serviceKey, address);
                
                // 使用调度器延迟注销（避免创建原始线程）
                scheduler.schedule(() -> {
                    try {
                        unregister(serviceKey, address);
                    } catch (Exception e) {
                        logger.error("Failed to unregister service during graceful shutdown: {}", serviceKey, e);
                    }
                }, drainTimeMs, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            logger.error("Failed to graceful unregister", e);
            unregister(serviceKey, address);
        }
    }
    
    @Override
    public List<String> discover(String serviceKey) {
        // 优先从缓存获取
        String cached = serviceCache.get(serviceKey);
        if (cached != null) {
            return Collections.singletonList(cached);
        }
        
        // 解析 serviceKey: interfaceName#serverId
        String[] parts = serviceKey.split("#");
        boolean isWildcard = parts.length == 2 && "0".equals(parts[1]);
        
        // 从ZooKeeper获取并建立监听
        try {
            List<String> allAddresses = new ArrayList<>();
            
            if (isWildcard) {
                // serverId=0 时，查找所有同接口的服务
                String interfacePrefix = parts[0]; // interfaceName
                allAddresses = discoverByInterfacePrefix(interfacePrefix);
            } else {
                // 精确匹配单个服务
                String servicePath = buildServicePath(serviceKey);
                
                if (client.checkExists().forPath(servicePath) != null) {
                    ServiceInstance instance = loadServiceInstance(serviceKey, servicePath);
                    if (instance != null && instance.getStatus() == ServiceStatus.UP) {
                        allAddresses.add(instance.getAddress());
                        serviceCache.put(serviceKey, instance.getAddress());
                    }
                    watchService(serviceKey);
                }
            }
            
            return allAddresses;
        } catch (Exception e) {
            logger.error("Failed to discover service from ZooKeeper", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 根据接口名前缀发现服务（用于 serverId=0 的情况）
     * 遍历所有匹配接口名的服务节点
     */
    private List<String> discoverByInterfacePrefix(String interfaceName) throws Exception {
        List<String> allAddresses = new ArrayList<>();
        String safePrefixPath = interfaceName.replace(".", "_").replace(":", "_");
        
        // 获取 /rpc 下所有服务节点
        if (client.checkExists().forPath(ROOT_PATH) == null) {
            return allAddresses;
        }
        
        List<String> serviceNodes = client.getChildren().forPath(ROOT_PATH);
        for (String nodeName : serviceNodes) {
            // 检查是否匹配前缀（如 com_example_IGameService_1 匹配 com_example_IGameService）
            if (nodeName.startsWith(safePrefixPath + "_")) {
                String fullPath = ROOT_PATH + "/" + nodeName;
                // 恢复原始 serviceKey
                String originalServiceKey = restoreServiceKey(nodeName);
                
                ServiceInstance instance = loadServiceInstance(originalServiceKey, fullPath);
                if (instance != null && instance.getStatus() == ServiceStatus.UP) {
                    allAddresses.add(instance.getAddress());
                    serviceCache.put(originalServiceKey, instance.getAddress());
                }
            }
        }
        
        return allAddresses;
    }
    
    /**
     * 加载单个服务实例
     */
    private ServiceInstance loadServiceInstance(String serviceKey, String path) {
        try {
            byte[] data = client.getData().forPath(path);
            if (data != null && data.length > 0) {
                ServiceInstance instance = JSON.parseObject(new String(data, StandardCharsets.UTF_8), ServiceInstance.class);
                // 使用可变 Map，支持后续更新
                Map<String, ServiceInstance> instanceMap = new HashMap<>();
                instanceMap.put(instance.getAddress(), instance);
                instanceCache.put(serviceKey, instanceMap);
                return instance;
            }
        } catch (Exception e) {
            logger.warn("Failed to load service instance: {}", path, e);
        }
        return null;
    }
    
    /**
     * 发现服务实例（带完整元数据）
     */
    public List<ServiceInstance> discoverInstances(String serviceKey) {
        // 解析 serviceKey: interfaceName#serverId
        String[] parts = serviceKey.split("#");
        boolean isWildcard = parts.length == 2 && "0".equals(parts[1]);
        
        List<ServiceInstance> result = new ArrayList<>();
        
        try {
            if (isWildcard) {
                // serverId=0 时，查找所有同接口的服务
                String interfaceName = parts[0];
                result = discoverInstancesByInterfacePrefix(interfaceName);
            } else {
                // 精确匹配单个服务
                String servicePath = buildServicePath(serviceKey);
                if (client.checkExists().forPath(servicePath) != null) {
                    ServiceInstance instance = loadServiceInstance(serviceKey, servicePath);
                    if (instance != null && instance.getStatus() == ServiceStatus.UP) {
                        result.add(instance);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to discover service instances", e);
        }
        
        return result;
    }
    
    /**
     * 根据接口名前缀发现服务实例
     */
    private List<ServiceInstance> discoverInstancesByInterfacePrefix(String interfaceName) throws Exception {
        List<ServiceInstance> instances = new ArrayList<>();
        String safePrefixPath = interfaceName.replace(".", "_").replace(":", "_");
        
        if (client.checkExists().forPath(ROOT_PATH) == null) {
            return instances;
        }
        
        List<String> serviceNodes = client.getChildren().forPath(ROOT_PATH);
        for (String nodeName : serviceNodes) {
            if (nodeName.startsWith(safePrefixPath + "_")) {
                String fullPath = ROOT_PATH + "/" + nodeName;
                String originalServiceKey = restoreServiceKey(nodeName);
                
                ServiceInstance instance = loadServiceInstance(originalServiceKey, fullPath);
                if (instance != null && instance.getStatus() == ServiceStatus.UP) {
                    instances.add(instance);
                }
            }
        }
        
        return instances;
    }
    
    /**
     * 根据区服ID发现服务
     */
    public List<String> discoverByZone(String serviceKey, String zoneId) {
        List<ServiceInstance> instances = discoverInstances(serviceKey);
        return instances.stream()
                .filter(i -> zoneId.equals(i.getMetadata(ServiceInstance.META_ZONE_ID)))
                .map(ServiceInstance::getAddress)
                .collect(Collectors.toList());
    }
    
    /**
     * 监听服务变化（线程安全）
     */
    private void watchService(String serviceKey) {
        // 使用 computeIfAbsent 确保原子性，避免重复创建 CuratorCache
        watcherCache.computeIfAbsent(serviceKey, key -> {
            String servicePath = buildServicePath(key);
            CuratorCache cache = CuratorCache.build(client, servicePath);
            
            CuratorCacheListener listener = createCacheListener(key, servicePath);
            cache.listenable().addListener(listener);
            cache.start();
            
            return cache;
        });
    }
    
    /**
     * 创建缓存监听器
     */
    private CuratorCacheListener createCacheListener(String serviceKey, String servicePath) {
        return CuratorCacheListener.builder()
                .forCreatesAndChanges((oldNode, newNode) -> {
                    logger.info("Service changed: {} - data updated", serviceKey);
                    try {
                        ServiceInstance instance = loadServiceInstance(serviceKey, servicePath);
                        if (instance != null && instance.getStatus() == ServiceStatus.UP) {
                            serviceCache.put(serviceKey, instance.getAddress());
                        } else {
                            serviceCache.remove(serviceKey);
                            notifyServiceOffline(serviceKey);
                        }
                        notifyListeners(serviceKey);
                        notifyInstanceListeners(serviceKey);
                    } catch (Exception e) {
                        logger.error("Failed to refresh service cache", e);
                    }
                })
                .forDeletes(oldNode -> {
                    logger.info("Service deleted: {}", serviceKey);
                    serviceCache.remove(serviceKey);
                    instanceCache.remove(serviceKey);
                    notifyServiceOffline(serviceKey);
                    notifyListeners(serviceKey);
                    notifyInstanceListeners(serviceKey);
                })
                .build();
    }
    
    
    /**
     * 通知服务下线
     */
    private void notifyServiceOffline(String serviceKey) {
        if (serviceOfflineCallback != null && serviceKey != null) {
            try {
                serviceOfflineCallback.accept(serviceKey);
            } catch (Exception e) {
                logger.error("Failed to notify service offline: {}", serviceKey, e);
            }
        }
    }
    
    /**
     * 订阅服务变更（地址列表）
     */
    public void subscribe(String serviceKey, Consumer<List<String>> listener) {
        listeners.computeIfAbsent(serviceKey, k -> new CopyOnWriteArrayList<>()).add(listener);
        
        // 立即触发一次
        List<String> addresses = discover(serviceKey);
        if (!addresses.isEmpty()) {
            listener.accept(addresses);
        }
    }
    
    /**
     * 订阅服务实例变更（带元数据）
     */
    public void subscribeInstances(String serviceKey, Consumer<List<ServiceInstance>> listener) {
        instanceListeners.computeIfAbsent(serviceKey, k -> new CopyOnWriteArrayList<>()).add(listener);
        
        // 立即触发一次
        List<ServiceInstance> instances = discoverInstances(serviceKey);
        if (!instances.isEmpty()) {
            listener.accept(instances);
        }
    }
    
    /**
     * 取消订阅
     */
    public void unsubscribe(String serviceKey, Consumer<List<String>> listener) {
        List<Consumer<List<String>>> list = listeners.get(serviceKey);
        if (list != null) {
            list.remove(listener);
        }
    }
    
    /**
     * 通知监听器
     */
    private void notifyListeners(String serviceKey) {
        List<Consumer<List<String>>> list = listeners.get(serviceKey);
        if (list != null) {
            List<String> addresses = discover(serviceKey);
            for (Consumer<List<String>> listener : list) {
                try {
                    listener.accept(new ArrayList<>(addresses));
                } catch (Exception e) {
                    logger.error("Notify listener error", e);
                }
            }
        }
    }
    
    /**
     * 通知服务实例监听器
     */
    private void notifyInstanceListeners(String serviceKey) {
        List<Consumer<List<ServiceInstance>>> list = instanceListeners.get(serviceKey);
        if (list != null) {
            List<ServiceInstance> instances = discoverInstances(serviceKey);
            for (Consumer<List<ServiceInstance>> listener : list) {
                try {
                    listener.accept(instances);
                } catch (Exception e) {
                    logger.error("Notify instance listener error", e);
                }
            }
        }
    }
    
    /**
     * 重连后重新注册服务
     */
    private void reRegisterServices() {
        logger.info("Re-registering services after reconnection...");
        for (ServiceInstance instance : registeredServices.values()) {
            try {
                registerInstance(instance);
            } catch (Exception e) {
                logger.error("Failed to re-register service: {}", instance, e);
            }
        }
    }
    
    /**
     * 构建服务路径
     * serviceKey格式: interfaceName#serverId
     * 路径格式: /rpc/interfaceName_serverId (将 . 和 # 替换为 _)
     */
    private String buildServicePath(String serviceKey) {
        // 将 . 和 # 替换为下划线，便于 ZooKeeper 路径处理
        String safePath = serviceKey.replace(".", "_").replace("#", "_").replace(":", "_");
        return ROOT_PATH + "/" + safePath;
    }
    
    /**
     * 从节点名还原 serviceKey
     * 节点格式: com_example_IGameService_1
     * serviceKey格式: com.example.IGameService#1
     */
    private String restoreServiceKey(String nodeName) {
        // 找到最后一个 _ 作为分隔符（serverId 之前）
        int lastUnderscore = nodeName.lastIndexOf('_');
        if (lastUnderscore > 0) {
            String interfacePart = nodeName.substring(0, lastUnderscore);
            String serverIdPart = nodeName.substring(lastUnderscore + 1);
            // 将接口名部分的 _ 还原为 .
            String interfaceName = interfacePart.replace("_", ".");
            return interfaceName + "#" + serverIdPart;
        }
        return nodeName;
    }
    
    /**
     * 确保路径存在
     */
    private void ensurePath(String path) throws Exception {
        if (client.checkExists().forPath(path) == null) {
            client.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath(path);
        }
    }
    
    /**
     * 获取服务实例
     */
    public ServiceInstance getServiceInstance(String serviceKey) {
        Map<String, ServiceInstance> instances = instanceCache.get(serviceKey);
        if (instances != null && !instances.isEmpty()) {
            return instances.values().iterator().next();
        }
        return null;
    }
    
    /**
     * 获取所有已缓存的服务（serviceKey -> address）
     */
    public Map<String, String> getAllServices() {
        return new HashMap<>(serviceCache);
    }
    
    /** 是否已关闭 */
    private volatile boolean closed = false;
    
    /**
     * 关闭注册中心
     */
    public synchronized void shutdown() {
        // 幂等检查，防止重复关闭
        if (closed) {
            return;
        }
        closed = true;
        
        logger.info("Shutting down ZooKeeper registry...");
        
        // 关闭调度器
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // 关闭所有监听器
        for (CuratorCache cache : watcherCache.values()) {
            cache.close();
        }
        watcherCache.clear();
        
        // 关闭客户端
        if (client != null) {
            client.close();
        }
        
        logger.info("ZooKeeper registry shutdown complete");
    }
    
    /**
     * 优雅关闭（注销所有服务后再关闭）
     * 异步执行，不阻塞调用线程
     */
    public void gracefulShutdown(long drainTimeMs) {
        logger.info("Graceful shutting down ZooKeeper registry...");
        
        // 标记所有服务为DRAINING状态
        for (ServiceInstance instance : registeredServices.values()) {
            try {
                instance.setStatus(ServiceStatus.DRAINING);
                registerInstance(instance);
            } catch (Exception e) {
                logger.error("Failed to set drain status", e);
            }
        }
        
        // 使用调度器延迟关闭，避免阻塞调用线程
        scheduler.schedule(() -> {
            try {
                shutdown();
            } catch (Exception e) {
                logger.error("Failed to shutdown during graceful shutdown", e);
            }
        }, drainTimeMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 优雅关闭（同步版本，会阻塞调用线程）
     */
    public void gracefulShutdownSync(long drainTimeMs) {
        logger.info("Graceful shutting down ZooKeeper registry (sync)...");
        
        // 标记所有服务为DRAINING状态
        for (ServiceInstance instance : registeredServices.values()) {
            try {
                instance.setStatus(ServiceStatus.DRAINING);
                registerInstance(instance);
            } catch (Exception e) {
                logger.error("Failed to set drain status", e);
            }
        }
        
        // 等待排空
        try {
            Thread.sleep(drainTimeMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 正常关闭
        shutdown();
    }
    
    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return client != null && client.getZookeeperClient().isConnected();
    }
    
    // ========== 配置方法 ==========
    
    public void setWeight(int weight) {
        this.weight = weight;
    }
    
    public void setServerId(String serverId) {
        this.serverId = serverId;
    }
    
    public void addMetadata(String key, String value) {
        this.metadata.put(key, value);
    }
    
    public int getWeight() {
        return weight;
    }
    
    public String getServerId() {
        return serverId;
    }
    
    /**
     * 设置服务下线回调
     * 当检测到服务下线时会调用此回调，传入下线服务的 serviceKey
     *
     * @param callback 回调函数，参数为下线服务的 serviceKey (interfaceName#serverId)
     */
    public void setServiceOfflineCallback(Consumer<String> callback) {
        this.serviceOfflineCallback = callback;
    }
}
