package com.muyi.rpc;

import com.muyi.rpc.registry.ZookeeperServiceRegistry;
import com.muyi.rpc.server.RpcServer;
import com.muyi.rpc.server.RpcServerConfig;

/**
 * 独立 RPC 服务端 Builder（不使用全局实例）
 * 通过 {@link Rpc#server(int)} 创建
 *
 * @author muyi
 */
public class ServerBuilder {
    
    private final int port;
    private int serverId = 1;
    private String zkAddress;
    private String host;
    private int weight = 100;
    private int idleTimeout = 60;
    private RpcServer server;
    private ZookeeperServiceRegistry registry;
    
    ServerBuilder(int port) {
        this.port = port;
    }
    
    public ServerBuilder serverId(int serverId) {
        this.serverId = serverId;
        return this;
    }
    
    public ServerBuilder zookeeper(String zkAddress) {
        this.zkAddress = zkAddress;
        return this;
    }
    
    public ServerBuilder host(String host) {
        this.host = host;
        return this;
    }
    
    public ServerBuilder weight(int weight) {
        this.weight = weight;
        return this;
    }
    
    public ServerBuilder idleTimeout(int seconds) {
        this.idleTimeout = seconds;
        return this;
    }
    
    /** 注册服务（可链式调用多次）*/
    public ServerBuilder register(Object service) {
        ensureCreated();
        server.registerService(service);
        return this;
    }
    
    /** 启动服务 */
    public RpcServer start() throws Exception {
        ensureCreated();
        server.start();
        return server;
    }
    
    private void ensureCreated() {
        if (server == null) {
            if (zkAddress == null) {
                throw new IllegalStateException("ZooKeeper address is required. Call zookeeper() first.");
            }
            registry = new ZookeeperServiceRegistry(zkAddress);
            registry.setWeight(weight);
            registry.setServerId(String.valueOf(serverId));
            
            server = new RpcServer(port, new RpcServerConfig().readerIdleTimeSeconds(idleTimeout))
                    .registry(registry)
                    .serverId(serverId);
            
            if (host != null) {
                server.host(host);
            }
        }
    }
}
