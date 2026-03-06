package com.muyi.rpc;

import com.muyi.rpc.client.RpcClientConfig;
import com.muyi.rpc.client.RpcProxyManager;
import com.muyi.rpc.registry.ZookeeperServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 独立 RPC 客户端 Builder（不使用全局实例）
 * 通过 {@link Rpc#client()} 创建
 *
 * @author muyi
 */
public class ClientBuilder {
    
    private static final Logger log = LoggerFactory.getLogger(ClientBuilder.class);
    
    private String zkAddress;
    private long timeout = 10_000;
    private int retries = 1;
    private int connectTimeout = 3_000;
    private int maxConnections = 10;
    
    ClientBuilder() {
    }
    
    public ClientBuilder zookeeper(String zkAddress) {
        this.zkAddress = zkAddress;
        return this;
    }
    
    public ClientBuilder timeout(long timeout) {
        this.timeout = timeout;
        return this;
    }
    
    public ClientBuilder retries(int retries) {
        this.retries = retries;
        return this;
    }
    
    public ClientBuilder connectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }
    
    public ClientBuilder maxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
        return this;
    }
    
    public RpcProxyManager connect() {
        if (zkAddress == null) {
            throw new IllegalStateException("ZooKeeper address is required. Call zookeeper() first.");
        }
        
        ZookeeperServiceRegistry registry = new ZookeeperServiceRegistry(zkAddress);
        
        RpcClientConfig clientConfig = new RpcClientConfig()
                .requestTimeout(timeout)
                .retries(retries)
                .connectTimeout(connectTimeout)
                .maxConnectionsPerAddress(maxConnections);
        
        RpcProxyManager manager = new RpcProxyManager()
                .discovery(registry)
                .clientConfig(clientConfig)
                .init();
        
        log.info("Created independent RpcProxyManager. Remember to call registry.shutdown() when done.");
        
        return manager;
    }
    
    /**
     * 创建客户端并返回包含 registry 的包装对象，方便统一关闭
     */
    public ClientConnection connectWithRegistry() {
        if (zkAddress == null) {
            throw new IllegalStateException("ZooKeeper address is required. Call zookeeper() first.");
        }
        
        ZookeeperServiceRegistry registry = new ZookeeperServiceRegistry(zkAddress);
        
        RpcClientConfig clientConfig = new RpcClientConfig()
                .requestTimeout(timeout)
                .retries(retries)
                .connectTimeout(connectTimeout)
                .maxConnectionsPerAddress(maxConnections);
        
        RpcProxyManager manager = new RpcProxyManager()
                .discovery(registry)
                .clientConfig(clientConfig)
                .init();
        
        return new ClientConnection(manager, registry);
    }
}
