package com.muyi.rpc.client.loadbalance;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询负载均衡策略
 *
 * @author muyi
 */
public class RoundRobinLoadBalance implements LoadBalance {
    
    /** 每个服务的计数器 */
    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();
    
    @Override
    public String select(List<String> addresses, String serviceKey) {
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }
        if (addresses.size() == 1) {
            return addresses.get(0);
        }
        
        AtomicInteger counter = counters.computeIfAbsent(serviceKey, k -> new AtomicInteger(0));
        
        // 使用位运算确保非负（处理整数溢出，Math.abs(Integer.MIN_VALUE) 仍为负数）
        int index = (counter.getAndIncrement() & Integer.MAX_VALUE) % addresses.size();
        
        return addresses.get(index);
    }
}

