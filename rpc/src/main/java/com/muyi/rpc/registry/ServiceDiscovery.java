package com.muyi.rpc.registry;

import java.util.List;

/**
 * 服务发现接口
 *
 * @author muyi
 */
public interface ServiceDiscovery {
    
    /**
     * 发现服务
     *
     * @param serviceKey 服务标识
     * @return 服务地址列表
     */
    List<String> discover(String serviceKey);
}

