package com.muyi.rpc;

/**
 * RPC 服务端链式调用 Builder
 * 通过 {@link Rpc#init(int, int, String)} 或 {@link Rpc#register(Object)} 创建
 *
 * @author muyi
 */
public class RpcBuilder {
    
    RpcBuilder() {
    }
    
    /** 注册服务 */
    public RpcBuilder register(Object service) {
        Rpc.register(service);
        return this;
    }
    
    /**
     * 设置主机地址
     * 必须在 register() 之前调用，否则不会生效
     */
    public RpcBuilder host(String host) {
        if (Rpc.serverInstance() != null) {
            throw new IllegalStateException("host() must be called before register(). Server already created.");
        }
        Rpc.setHostInternal(host);
        return this;
    }
    
    /**
     * 设置权重
     * 必须在 register() 之前调用，否则不会生效
     */
    public RpcBuilder weight(int weight) {
        if (Rpc.serverInstance() != null) {
            throw new IllegalStateException("weight() must be called before register(). Server already created.");
        }
        Rpc.setWeightInternal(weight);
        return this;
    }
    
    /** 启动服务端 */
    public void start() throws Exception {
        Rpc.start();
    }
}
