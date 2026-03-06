package com.muyi.rpc;

import com.muyi.rpc.client.RpcProxyManager;
import com.muyi.rpc.registry.ZookeeperServiceRegistry;

/**
 * RPC 客户端连接包装类
 * 包含 RpcProxyManager 和 ZookeeperServiceRegistry，用于统一管理资源关闭
 *
 * @author muyi
 */
public class ClientConnection implements AutoCloseable {
    
    private final RpcProxyManager proxyManager;
    private final ZookeeperServiceRegistry registry;
    
    ClientConnection(RpcProxyManager proxyManager, ZookeeperServiceRegistry registry) {
        this.proxyManager = proxyManager;
        this.registry = registry;
    }
    
    public RpcProxyManager getProxyManager() {
        return proxyManager;
    }
    
    public ZookeeperServiceRegistry getRegistry() {
        return registry;
    }
    
    @Override
    public void close() {
        if (proxyManager != null) {
            proxyManager.shutdown();
        }
        if (registry != null) {
            registry.shutdown();
        }
    }
    
    public void shutdown() {
        close();
    }
}
