package com.muyi.rpc.client;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.muyi.rpc.core.ServiceKey;
import com.muyi.rpc.registry.ServiceDiscovery;
import com.muyi.rpc.registry.ServiceInstance;
import com.muyi.rpc.registry.ZookeeperServiceRegistry;

/**
 * RPC 代理管理器
 * 游戏服务器分服场景下的服务代理获取，专为 SLG 类型游戏优化
 * 
 * 使用示例:
 * <pre>
 * RpcProxyManager proxyManager = new RpcProxyManager()
 *     .discovery(zookeeperRegistry)
 *     .init();
 * 
 * // 1. 获取服务代理（负载均衡选择一个实例）
 * IWorldService worldService = proxyManager.get(IWorldService.class);
 * 
 * // 2. 跨服调用：获取指定服务器实例
 * IGameService game2 = proxyManager.get(IGameService.class, 2);
 * 
 * // 3. 随机选择一个 gate 服务
 * IGateService gate = proxyManager.getRandom(IGateService.class);
 * 
 * // 4. 按权重选择服务
 * IGameService game = proxyManager.getByWeight(IGameService.class);
 * 
 * // 5. 获取指定区服的服务
 * IGameService zoneGame = proxyManager.getByZone(IGameService.class, "zone_1");
 * 
 * // 6. 获取所有服务实例信息
 * List&lt;ServiceInstance&gt; gates = proxyManager.getInstances(IGateService.class);
 * </pre>
 *
 * @author muyi
 */
public class RpcProxyManager {
    
    private static final Logger logger = LoggerFactory.getLogger(RpcProxyManager.class);
    
    /** 服务发现 */
    private ServiceDiscovery discovery;
    
    /** RPC 客户端 */
    private RpcClient rpcClient;
    
    /** RPC 代理工厂 */
    private RpcProxy rpcProxy;
    
    /** 服务选择器 */
    private ServiceSelector serviceSelector;
    
    /** 
     * 代理缓存: key = interface#serverId 
     * 注意：只缓存 serverId=0（负载均衡）的代理，这类代理每次调用时会重新选择地址
     * 指定 serverId 的代理不缓存，避免服务下线后调用失败
     */
    private final Map<String, Object> proxyCache = new ConcurrentHashMap<>();
    
    /** 是否已初始化 */
    private volatile boolean initialized = false;
    
    /** 是否已关闭 */
    private volatile boolean shutdown = false;
    
    /** 请求超时（毫秒） */
    private long requestTimeout = 10_000;
    
    /** 连接超时（毫秒） */
    private int connectTimeout = 3_000;
    
    /** 重试次数 */
    private int retries = 0;
    
    /** 每个地址最大连接数 */
    private int maxConnectionsPerAddress = 10;
    
    /**
     * 设置服务发现
     */
    public RpcProxyManager discovery(ServiceDiscovery discovery) {
        this.discovery = discovery;
        return this;
    }
    
    /**
     * 设置请求超时
     */
    public RpcProxyManager requestTimeout(long timeout) {
        this.requestTimeout = timeout;
        return this;
    }
    
    /**
     * 设置连接超时
     */
    public RpcProxyManager connectTimeout(int timeout) {
        this.connectTimeout = timeout;
        return this;
    }
    
    /**
     * 设置重试次数
     */
    public RpcProxyManager retries(int retries) {
        this.retries = retries;
        return this;
    }
    
    /**
     * 设置每个地址最大连接数
     */
    public RpcProxyManager maxConnectionsPerAddress(int max) {
        this.maxConnectionsPerAddress = max;
        return this;
    }
    
    /**
     * 初始化（只能调用一次）
     */
    public RpcProxyManager init() {
        if (initialized) {
            logger.warn("[RpcProxyManager] Already initialized, skipping...");
            return this;
        }
        
        logger.info("[RpcProxyManager] Initializing...");
        
        if (discovery == null) {
            throw new IllegalStateException("ServiceDiscovery is required. Call discovery() first.");
        }
        
        rpcClient = new RpcClient()
                .discovery(discovery)
                .connectTimeout(connectTimeout)
                .maxConnectionsPerAddress(maxConnectionsPerAddress);
        
        rpcProxy = new RpcProxy(rpcClient);
        serviceSelector = new ServiceSelector(discovery);
        
        // 如果是 ZookeeperServiceRegistry，设置服务下线回调
        if (discovery instanceof ZookeeperServiceRegistry zkRegistry) {
            zkRegistry.setServiceOfflineCallback(this::onServiceOffline);
            logger.info("[RpcProxyManager] Registered service offline callback");
        }
        
        initialized = true;
        logger.info("[RpcProxyManager] Initialized with service discovery");
        return this;
    }
    
    /**
     * 检查是否可用（已初始化且未关闭）
     */
    private void checkState() {
        if (!initialized) {
            throw new IllegalStateException("[RpcProxyManager] Not initialized. Call init() first.");
        }
        if (shutdown) {
            throw new IllegalStateException("[RpcProxyManager] Already shutdown.");
        }
    }
    
    /**
     * 服务下线回调
     */
    private void onServiceOffline(String serviceKey) {
        invalidateByServiceKey(serviceKey);
        logger.info("[RpcProxyManager] Service offline, invalidated cache: {}", serviceKey);
    }
    
    /**
     * 获取服务代理（负载均衡选择实例）
     * 
     * @param serviceClass 服务接口类
     */
    public <T> T get(Class<T> serviceClass) {
        checkState();
        return get(serviceClass, 0);
    }
    
    /**
     * 获取指定服务器实例的代理（跨服调用）
     * 
     * @param serviceClass 服务接口类
     * @param serverId     服务器ID（如 1, 2, 3），0 表示负载均衡选择
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> serviceClass, int serverId) {
        String cacheKey = buildCacheKey(serviceClass, serverId);
        
        if (serverId == 0) {
            // serverId=0 表示负载均衡，代理内部每次调用会重新选择地址，可以安全缓存
            return (T) proxyCache.computeIfAbsent(cacheKey, key -> {
                logger.info("[RpcProxyManager] Creating load-balance proxy for: {}", cacheKey);
                return rpcProxy.create(serviceClass, serverId, requestTimeout, retries);
            });
        } else {
            // 指定 serverId 时，先检查服务是否可用
            ServiceInstance instance = serviceSelector.selectByServerId(serviceClass, serverId);
            if (instance == null) {
                logger.warn("Service not available: {}#{}", serviceClass.getSimpleName(), serverId);
                return null;
            }
            // 缓存指定 serverId 的代理
            return (T) proxyCache.computeIfAbsent(cacheKey, key -> {
                logger.info("[RpcProxyManager] Creating proxy for: {}", cacheKey);
                return rpcProxy.create(serviceClass, serverId, requestTimeout, retries);
            });
        }
    }
    
    /**
     * 构建缓存 key
     * 注意：需要与 RpcProxy 中的缓存 key 格式一致，包含 timeout 和 retries
     */
    private String buildCacheKey(Class<?> serviceClass, int serverId) {
        return serviceClass.getName() + "#" + serverId + "#" + requestTimeout + "#" + retries;
    }
    
    // ==================== SLG 游戏便捷 API ====================
    
    /**
     * 随机选择一个服务代理
     * 适用于无状态服务，如 gate、chat 等
     *
     * @param serviceClass 服务接口类
     * @return 服务代理，没有可用实例返回 null
     */
    public <T> T getRandom(Class<T> serviceClass) {
        checkState();
        ServiceInstance instance = serviceSelector.selectRandom(serviceClass);
        return getProxyFromInstance(serviceClass, instance);
    }
    
    /**
     * 按权重选择一个服务代理
     * 适用于需要按权重分配流量的场景
     *
     * @param serviceClass 服务接口类
     * @return 服务代理，没有可用实例返回 null
     */
    public <T> T getByWeight(Class<T> serviceClass) {
        checkState();
        ServiceInstance instance = serviceSelector.selectByWeight(serviceClass);
        return getProxyFromInstance(serviceClass, instance);
    }
    
    /**
     * 轮询选择一个服务代理
     * 适用于需要均匀分配请求的场景
     *
     * @param serviceClass 服务接口类
     * @return 服务代理，没有可用实例返回 null
     */
    public <T> T getRoundRobin(Class<T> serviceClass) {
        checkState();
        ServiceInstance instance = serviceSelector.selectRoundRobin(serviceClass);
        return getProxyFromInstance(serviceClass, instance);
    }
    
    /**
     * 根据区服ID获取服务代理
     * 适用于 SLG 游戏的分区分服架构
     *
     * @param serviceClass 服务接口类
     * @param zoneId       区服ID
     * @return 服务代理，没有找到返回 null
     */
    public <T> T getByZone(Class<T> serviceClass, String zoneId) {
        checkState();
        ServiceInstance instance = serviceSelector.selectByZone(serviceClass, zoneId);
        if (instance == null) {
            logger.warn("No available service for zone: {} ({})", zoneId, serviceClass.getSimpleName());
            return null;
        }
        return getProxyFromInstance(serviceClass, instance);
    }
    
    /**
     * 根据负载选择服务代理（选择负载最低的）
     * 适用于需要均衡负载的场景
     *
     * @param serviceClass 服务接口类
     * @return 服务代理，没有可用实例返回 null
     */
    public <T> T getByLoad(Class<T> serviceClass) {
        checkState();
        ServiceInstance instance = serviceSelector.selectByLoad(serviceClass);
        return getProxyFromInstance(serviceClass, instance);
    }
    
    /**
     * 从 ServiceInstance 获取代理
     * 从 serviceKey 解析 serverId，然后调用 get() 方法
     */
    private <T> T getProxyFromInstance(Class<T> serviceClass, ServiceInstance instance) {
        if (instance == null) {
            logger.warn("No available service for: {}", serviceClass.getSimpleName());
            return null;
        }
        // 从 serviceKey 解析 serverId
        int serverId = ServiceKey.parseServerId(instance.getServiceKey());
        return get(serviceClass, serverId);
    }
    
    /**
     * 使指定 serviceKey 的代理缓存失效
     * 当服务下线时调用
     *
     * @param serviceKey 服务标识 (interfaceName#serverId)
     */
    public void invalidateByServiceKey(String serviceKey) {
        // 缓存 key 格式为: interfaceName#serverId#timeout#retries
        // serviceKey 格式为: interfaceName#serverId
        // 需要匹配前缀来删除所有相关缓存
        String prefix = serviceKey + "#";
        int removed = 0;
        var iterator = proxyCache.entrySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next().getKey();
            if (key.startsWith(prefix)) {
                iterator.remove();
                removed++;
            }
        }
        if (removed > 0) {
            logger.info("[RpcProxyManager] Invalidated {} cache entries for serviceKey: {}", removed, serviceKey);
        }
    }
    
    /**
     * 使指定服务的所有代理缓存失效
     *
     * @param serviceClass 服务接口类
     */
    public void invalidateService(Class<?> serviceClass) {
        String prefix = serviceClass.getName() + "#";
        proxyCache.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix));
        logger.info("[RpcProxyManager] Invalidated cache for service: {}", serviceClass.getSimpleName());
    }
    
    /**
     * 清空所有代理缓存
     */
    public void invalidateAll() {
        proxyCache.clear();
        logger.info("[RpcProxyManager] Invalidated all proxy cache");
    }
    
    // ==================== 服务实例查询 API ====================
    
    /**
     * 获取某服务的所有实例信息
     *
     * @param serviceClass 服务接口类
     * @return 服务实例列表
     */
    public List<ServiceInstance> getInstances(Class<?> serviceClass) {
        checkState();
        return serviceSelector.selectAll(serviceClass);
    }
    
    /**
     * 获取某区服的所有服务实例
     *
     * @param serviceClass 服务接口类
     * @param zoneId       区服ID
     * @return 服务实例列表
     */
    public List<ServiceInstance> getInstancesByZone(Class<?> serviceClass, String zoneId) {
        checkState();
        return serviceSelector.selectAllByZone(serviceClass, zoneId);
    }
    
    /**
     * 获取某服务的所有地址
     *
     * @param serviceClass 服务接口类
     * @return 地址列表
     */
    public List<String> getAddresses(Class<?> serviceClass) {
        checkState();
        return serviceSelector.getAddresses(serviceClass);
    }
    
    /**
     * 获取某服务的实例数量
     *
     * @param serviceClass 服务接口类
     * @return 实例数量
     */
    public int getInstanceCount(Class<?> serviceClass) {
        checkState();
        return serviceSelector.getInstanceCount(serviceClass);
    }
    
    /**
     * 检查是否有可用的服务实例
     *
     * @param serviceClass 服务接口类
     * @return 是否有可用实例
     */
    public boolean hasAvailableInstance(Class<?> serviceClass) {
        checkState();
        return serviceSelector.hasAvailableInstance(serviceClass);
    }
    
    /**
     * 获取服务选择器（用于高级自定义选择逻辑）
     *
     * @return 服务选择器
     */
    public ServiceSelector getServiceSelector() {
        return serviceSelector;
    }
    
    /**
     * 关闭
     */
    public void shutdown() {
        if (shutdown) {
            return;
        }
        shutdown = true;
        
        logger.info("[RpcProxyManager] Shutting down...");
        if (rpcClient != null) {
            rpcClient.shutdown();
            rpcClient = null;
        }
        proxyCache.clear();
        logger.info("[RpcProxyManager] Shutdown complete");
    }
}
