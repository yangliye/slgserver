package com.muyi.rpc.client.loadbalance;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机负载均衡策略
 *
 * @author muyi
 */
public class RandomLoadBalance implements LoadBalance {
    
    @Override
    public String select(List<String> addresses, String serviceKey) {
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }
        if (addresses.size() == 1) {
            return addresses.get(0);
        }
        return addresses.get(ThreadLocalRandom.current().nextInt(addresses.size()));
    }
}

