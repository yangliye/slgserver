package com.muyi.rpc.client;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC服务代理工厂
 * 创建远程服务的本地代理
 * 
 * 调用模式自动判断：
 * - 返回值为 void：单向调用（不等待响应）
 * - 返回值为 RpcFuture/CompletableFuture/Future：异步调用
 * - 其他返回值：同步调用
 *
 * @author muyi
 */
public class RpcProxy {
    
    private static final Logger logger = LoggerFactory.getLogger(RpcProxy.class);
    
    private final RpcClient client;
    
    /** 代理缓存 */
    private final Map<String, Object> proxyCache = new ConcurrentHashMap<>();
    
    public RpcProxy(RpcClient client) {
        this.client = client;
    }
    
    /**
     * 创建服务代理（使用默认配置）
     */
    public <T> T create(Class<T> interfaceClass) {
        return create(interfaceClass, 0, 5000, 2);
    }
    
    /**
     * 创建服务代理
     * 
     * @param interfaceClass 服务接口类
     * @param serverId       服务器ID（0 表示不指定，由负载均衡选择）
     */
    public <T> T create(Class<T> interfaceClass, int serverId) {
        return create(interfaceClass, serverId, 5000, 2);
    }
    
    /**
     * 创建服务代理（完整配置）
     * 
     * @param interfaceClass 服务接口类
     * @param serverId       服务器ID（0 表示不指定，由负载均衡选择）
     * @param timeout        超时时间（毫秒）
     * @param retries        重试次数
     */
    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> interfaceClass, int serverId, long timeout, int retries) {
        // 缓存 key 包含所有配置参数，确保不同配置使用不同代理
        String cacheKey = interfaceClass.getName() + "#" + serverId + "#" + timeout + "#" + retries;
        
        return (T) proxyCache.computeIfAbsent(cacheKey, key -> {
            // 获取类加载器，如果为 null（bootstrap classloader）则使用当前线程上下文类加载器
            ClassLoader classLoader = interfaceClass.getClassLoader();
            if (classLoader == null) {
                classLoader = Thread.currentThread().getContextClassLoader();
            }
            if (classLoader == null) {
                classLoader = RpcProxy.class.getClassLoader();
            }
            return Proxy.newProxyInstance(
                    classLoader,
                    new Class<?>[]{interfaceClass},
                    new RpcInvocationHandler(interfaceClass, serverId, timeout, retries, client)
            );
        });
    }
    
    /**
     * 获取RpcClient
     */
    public RpcClient getClient() {
        return client;
    }
    
    /**
     * 清空代理缓存
     */
    public void clearCache() {
        proxyCache.clear();
    }
    
    /**
     * 获取缓存大小
     */
    public int getCacheSize() {
        return proxyCache.size();
    }
}
