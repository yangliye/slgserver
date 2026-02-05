package com.muyi.rpc.registry;

/**
 * 服务注册接口
 *
 * @author muyi
 */
public interface ServiceRegistry {
    
    /**
     * 注册服务
     *
     * @param serviceKey 服务标识
     * @param address    服务地址（host:port）
     */
    void register(String serviceKey, String address);
    
    /**
     * 注销服务
     *
     * @param serviceKey 服务标识
     * @param address    服务地址
     */
    void unregister(String serviceKey, String address);
}

