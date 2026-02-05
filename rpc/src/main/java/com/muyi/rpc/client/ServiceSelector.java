package com.muyi.rpc.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.muyi.rpc.client.loadbalance.LoadBalance;
import com.muyi.rpc.client.loadbalance.RandomLoadBalance;
import com.muyi.rpc.client.loadbalance.WeightedRandomLoadBalance;
import com.muyi.rpc.core.ServiceKey;
import com.muyi.rpc.registry.ServiceDiscovery;
import com.muyi.rpc.registry.ServiceInstance;
import com.muyi.rpc.registry.ZookeeperServiceRegistry;

/**
 * 服务选择器
 * 
 * 封装服务发现和负载均衡逻辑，为 SLG 游戏提供便捷的服务选择 API
 * 
 * 使用示例:
 * <pre>
 * ServiceSelector selector = new ServiceSelector(registry);
 * 
 * // 随机选择一个 gate 服务
 * ServiceInstance gate = selector.selectRandom(IGateService.class);
 * 
 * // 按权重选择一个 game 服务
 * ServiceInstance game = selector.selectByWeight(IGameService.class);
 * 
 * // 获取指定区服的服务
 * ServiceInstance zoneGame = selector.selectByZone(IGameService.class, "zone_1");
 * 
 * // 获取所有 world 服务
 * List<ServiceInstance> worlds = selector.selectAll(IWorldService.class);
 * </pre>
 *
 * @author muyi
 */
public class ServiceSelector {
    
    private static final Logger logger = LoggerFactory.getLogger(ServiceSelector.class);
    
    /** 服务发现 */
    private final ServiceDiscovery discovery;
    
    /** ZooKeeper 注册中心（用于获取完整服务实例信息） */
    private final ZookeeperServiceRegistry registry;
    
    /** 随机负载均衡 */
    private final LoadBalance randomLoadBalance = new RandomLoadBalance();
    
    /** 权重负载均衡 */
    private final LoadBalance weightedLoadBalance = new WeightedRandomLoadBalance();
    
    /** 轮询计数器 */
    private final Map<String, Integer> roundRobinCounters = new ConcurrentHashMap<>();
    
    /**
     * 创建服务选择器
     *
     * @param discovery 服务发现（通常是 ZookeeperServiceRegistry）
     */
    public ServiceSelector(ServiceDiscovery discovery) {
        this.discovery = discovery;
        this.registry = (discovery instanceof ZookeeperServiceRegistry) 
                ? (ZookeeperServiceRegistry) discovery : null;
    }
    
    // ==================== 核心选择方法 ====================
    
    /**
     * 随机选择一个服务实例
     *
     * @param serviceClass 服务接口类
     * @return 服务实例，没有可用实例返回 null
     */
    public ServiceInstance selectRandom(Class<?> serviceClass) {
        List<ServiceInstance> instances = selectAll(serviceClass);
        if (instances.isEmpty()) {
            return null;
        }
        return instances.get(ThreadLocalRandom.current().nextInt(instances.size()));
    }
    
    /**
     * 按权重选择一个服务实例
     *
     * @param serviceClass 服务接口类
     * @return 服务实例，没有可用实例返回 null
     */
    public ServiceInstance selectByWeight(Class<?> serviceClass) {
        List<ServiceInstance> instances = selectAll(serviceClass);
        String serviceKey = serviceClass.getName();
        return weightedLoadBalance.selectInstance(instances, serviceKey);
    }
    
    /**
     * 轮询选择一个服务实例
     *
     * @param serviceClass 服务接口类
     * @return 服务实例，没有可用实例返回 null
     */
    public ServiceInstance selectRoundRobin(Class<?> serviceClass) {
        List<ServiceInstance> instances = selectAll(serviceClass);
        if (instances.isEmpty()) {
            return null;
        }
        
        int size = instances.size();
        String key = serviceClass.getName();
        
        // 先递增计数器，再对当前 size 取模（避免 compute 中使用 size 导致并发问题）
        int index = roundRobinCounters.compute(key, (k, v) -> (v == null) ? 0 : v + 1);
        
        // 使用位运算确保非负，然后对 size 取模
        return instances.get((index & Integer.MAX_VALUE) % size);
    }
    
    /**
     * 根据服务器ID选择指定实例
     *
     * @param serviceClass 服务接口类
     * @param serverId     服务器ID
     * @return 服务实例，没有找到返回 null
     */
    public ServiceInstance selectByServerId(Class<?> serviceClass, int serverId) {
        String serviceKey = ServiceKey.build(serviceClass.getName(), serverId);
        List<String> addresses = discovery.discover(serviceKey);
        
        if (addresses.isEmpty()) {
            logger.warn("No service found for: {}", serviceKey);
            return null;
        }
        
        // 获取完整实例信息
        if (registry != null) {
            List<ServiceInstance> instances = registry.discoverInstances(serviceKey);
            if (!instances.isEmpty()) {
                return instances.get(0);
            }
        }
        
        // 只有地址信息时创建简单实例
        return new ServiceInstance(serviceKey, addresses.get(0));
    }
    
    /**
     * 根据区服ID选择服务实例
     *
     * @param serviceClass 服务接口类
     * @param zoneId       区服ID
     * @return 服务实例，没有找到返回 null
     */
    public ServiceInstance selectByZone(Class<?> serviceClass, String zoneId) {
        List<ServiceInstance> zoneInstances = selectAllByZone(serviceClass, zoneId);
        if (zoneInstances.isEmpty()) {
            logger.warn("No service found for zone: {}", zoneId);
            return null;
        }
        // 区服内随机选择
        return zoneInstances.get(ThreadLocalRandom.current().nextInt(zoneInstances.size()));
    }
    
    /**
     * 根据负载选择服务实例（选择负载最低的）
     *
     * @param serviceClass 服务接口类
     * @return 服务实例，没有可用实例返回 null
     */
    public ServiceInstance selectByLoad(Class<?> serviceClass) {
        List<ServiceInstance> instances = selectAll(serviceClass);
        if (instances.isEmpty()) {
            return null;
        }
        
        // 按负载排序（负载越低越优先）
        return instances.stream()
                .min((a, b) -> {
                    int loadA = parseLoad(a);
                    int loadB = parseLoad(b);
                    return Integer.compare(loadA, loadB);
                })
                .orElse(instances.get(0));
    }
    
    // ==================== 批量获取方法 ====================
    
    /**
     * 获取某服务的所有实例
     *
     * @param serviceClass 服务接口类
     * @return 服务实例列表（不可变）
     */
    public List<ServiceInstance> selectAll(Class<?> serviceClass) {
        // 使用 serverId=0 查询所有同类型服务
        String serviceKey = ServiceKey.build(serviceClass.getName(), 0);
        
        if (registry != null) {
            List<ServiceInstance> instances = registry.discoverInstances(serviceKey);
            if (!instances.isEmpty()) {
                return Collections.unmodifiableList(instances);
            }
        }
        
        // 退化为地址列表
        List<String> addresses = discovery.discover(serviceKey);
        if (addresses.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 转换为简单实例
        List<ServiceInstance> instances = new ArrayList<>();
        for (String address : addresses) {
            instances.add(new ServiceInstance(serviceKey, address));
        }
        return Collections.unmodifiableList(instances);
    }
    
    /**
     * 获取某区服的所有服务实例
     *
     * @param serviceClass 服务接口类
     * @param zoneId       区服ID
     * @return 服务实例列表
     */
    public List<ServiceInstance> selectAllByZone(Class<?> serviceClass, String zoneId) {
        List<ServiceInstance> allInstances = selectAll(serviceClass);
        return allInstances.stream()
                .filter(i -> zoneId.equals(i.getMetadata(ServiceInstance.META_ZONE_ID)))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有可用服务的地址
     *
     * @param serviceClass 服务接口类
     * @return 地址列表
     */
    public List<String> getAddresses(Class<?> serviceClass) {
        return selectAll(serviceClass).stream()
                .map(ServiceInstance::getAddress)
                .collect(Collectors.toList());
    }
    
    // ==================== 统计方法 ====================
    
    /**
     * 获取某服务的实例数量
     *
     * @param serviceClass 服务接口类
     * @return 实例数量
     */
    public int getInstanceCount(Class<?> serviceClass) {
        return selectAll(serviceClass).size();
    }
    
    /**
     * 检查是否有可用的服务实例
     *
     * @param serviceClass 服务接口类
     * @return 是否有可用实例
     */
    public boolean hasAvailableInstance(Class<?> serviceClass) {
        return !selectAll(serviceClass).isEmpty();
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 解析服务实例的负载值
     * 没有负载信息时返回 Integer.MAX_VALUE，避免被优先选择
     */
    private int parseLoad(ServiceInstance instance) {
        String loadStr = instance.getMetadata(ServiceInstance.META_LOAD);
        if (loadStr != null && !loadStr.isEmpty()) {
            try {
                return Integer.parseInt(loadStr);
            } catch (NumberFormatException e) {
                // 解析失败，返回最大值表示未知负载
            }
        }
        // 没有负载信息时返回最大值，不会被"负载最低"优先选中
        return Integer.MAX_VALUE;
    }
}
