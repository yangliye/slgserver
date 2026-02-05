package com.muyi.rpc.client.loadbalance;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.muyi.rpc.registry.ServiceInstance;

/**
 * 权重随机负载均衡策略
 * 
 * 根据服务实例的权重进行加权随机选择，权重越高被选中的概率越大
 * 
 * 使用示例:
 * <pre>
 * // 假设有3个服务实例，权重分别为 100, 200, 300
 * // 被选中的概率分别为: 100/600=16.7%, 200/600=33.3%, 300/600=50%
 * </pre>
 *
 * @author muyi
 */
public class WeightedRandomLoadBalance implements LoadBalance {
    
    @Override
    public String select(List<String> addresses, String serviceKey) {
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }
        if (addresses.size() == 1) {
            return addresses.get(0);
        }
        // 无权重信息时退化为随机选择
        return addresses.get(ThreadLocalRandom.current().nextInt(addresses.size()));
    }
    
    @Override
    public ServiceInstance selectInstance(List<ServiceInstance> instances, String serviceKey) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        if (instances.size() == 1) {
            return instances.get(0);
        }
        
        // 计算总权重
        int totalWeight = 0;
        boolean sameWeight = true;
        int firstWeight = Math.max(instances.get(0).getWeight(), 1); // 保持一致：权重至少为1
        
        for (ServiceInstance instance : instances) {
            int weight = Math.max(instance.getWeight(), 1); // 权重至少为1
            totalWeight += weight;
            if (sameWeight && weight != firstWeight) {
                sameWeight = false;
            }
        }
        
        // 权重相同时使用随机选择
        if (sameWeight) {
            return instances.get(ThreadLocalRandom.current().nextInt(instances.size()));
        }
        
        // 加权随机选择
        int randomWeight = ThreadLocalRandom.current().nextInt(totalWeight);
        for (ServiceInstance instance : instances) {
            int weight = Math.max(instance.getWeight(), 1);
            randomWeight -= weight;
            if (randomWeight < 0) {
                return instance;
            }
        }
        
        // 理论上不会执行到这里（randomWeight < totalWeight 保证循环内一定 return）
        // 但为了编译器和防御性编程，保留兜底
        return instances.get(instances.size() - 1);
    }
}
