package com.muyi.rpc.client.loadbalance;

import java.util.List;

import com.muyi.rpc.registry.ServiceInstance;

/**
 * 负载均衡策略接口
 * 
 * 支持两种模式:
 * - 基于地址列表的简单选择
 * - 基于服务实例的智能选择（支持权重、元数据等）
 *
 * @author muyi
 */
public interface LoadBalance {
    
    /**
     * 从服务地址列表中选择一个
     *
     * @param addresses  服务地址列表
     * @param serviceKey 服务标识
     * @return 选中的地址
     */
    String select(List<String> addresses, String serviceKey);
    
    /**
     * 从服务实例列表中选择一个（支持权重等高级特性）
     *
     * @param instances  服务实例列表
     * @param serviceKey 服务标识
     * @return 选中的服务实例，如果列表为空返回 null
     */
    default ServiceInstance selectInstance(List<ServiceInstance> instances, String serviceKey) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        // 默认实现：使用 select 方法选择地址后匹配实例
        String address = select(
                instances.stream().map(ServiceInstance::getAddress).toList(),
                serviceKey
        );
        return instances.stream()
                .filter(i -> i.getAddress().equals(address))
                .findFirst()
                .orElse(instances.get(0));
    }
}

