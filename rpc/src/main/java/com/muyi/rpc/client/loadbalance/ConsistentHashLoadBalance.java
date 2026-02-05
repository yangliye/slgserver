package com.muyi.rpc.client.loadbalance;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 一致性哈希负载均衡策略
 * 适用于游戏场景中需要同一玩家请求落到同一服务器的场景
 *
 * @author muyi
 */
public class ConsistentHashLoadBalance implements LoadBalance {
    
    /** 虚拟节点数量 */
    private static final int VIRTUAL_NODES = 160;
    
    /** 缓存最大容量（防止无限增长导致内存泄漏） */
    private static final int MAX_CACHE_SIZE = 1000;
    
    /** 哈希环缓存：cacheKey -> hashRing（避免每次调用重建哈希环） */
    private final Map<String, TreeMap<Long, String>> ringCache = new ConcurrentHashMap<>();
    
    /** ThreadLocal 缓存 MessageDigest，避免每次创建（MD5 不是线程安全的） */
    private static final ThreadLocal<MessageDigest> MD5_DIGEST = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    });
    
    @Override
    public String select(List<String> addresses, String serviceKey) {
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }
        if (addresses.size() == 1) {
            return addresses.get(0);
        }
        
        // 生成缓存key（排序后计算哈希，使用两种哈希算法减少碰撞风险）
        String sortedAddresses = addresses.stream().sorted().collect(Collectors.joining(","));
        // 使用 hashCode + 内容长度 + 首尾字符作为 key，降低碰撞概率
        int hash1 = sortedAddresses.hashCode();
        int hash2 = sortedAddresses.length() > 0 ? 
                (sortedAddresses.charAt(0) * 31 + sortedAddresses.charAt(sortedAddresses.length() - 1)) : 0;
        String cacheKey = hash1 + "_" + hash2 + "_" + addresses.size();
        
        // 缓存满时清理（LRU 近似策略：清除一半旧数据）
        if (ringCache.size() >= MAX_CACHE_SIZE) {
            // 移除约一半的缓存条目，减少缓存抖动
            int toRemove = ringCache.size() / 2;
            var iterator = ringCache.keySet().iterator();
            while (iterator.hasNext() && toRemove > 0) {
                iterator.next();
                iterator.remove();
                toRemove--;
            }
        }
        
        // 从缓存获取或构建哈希环
        TreeMap<Long, String> hashRing = ringCache.computeIfAbsent(cacheKey, k -> buildHashRing(addresses));
        
        // 计算serviceKey的哈希值
        long hash = hash(md5(serviceKey), 0);
        
        // 找到第一个大于等于该哈希值的节点
        SortedMap<Long, String> tailMap = hashRing.tailMap(hash);
        Long nodeHash = tailMap.isEmpty() ? hashRing.firstKey() : tailMap.firstKey();
        
        return hashRing.get(nodeHash);
    }
    
    /**
     * 构建哈希环
     */
    private TreeMap<Long, String> buildHashRing(List<String> addresses) {
        TreeMap<Long, String> hashRing = new TreeMap<>();
        for (String address : addresses) {
            for (int i = 0; i < VIRTUAL_NODES / 4; i++) {
                byte[] digest = md5(address + "#" + i);
                for (int h = 0; h < 4; h++) {
                    long hash = hash(digest, h);
                    hashRing.put(hash, address);
                }
            }
        }
        return hashRing;
    }
    
    /**
     * 清除缓存（当服务列表发生变化时可调用）
     */
    public void clearCache() {
        ringCache.clear();
    }
    
    /**
     * MD5哈希（使用 ThreadLocal 缓存 MessageDigest）
     */
    private byte[] md5(String key) {
        MessageDigest md = MD5_DIGEST.get();
        md.reset(); // 重置以便复用
        return md.digest(key.getBytes());
    }
    
    /**
     * 从MD5结果计算哈希值
     */
    private long hash(byte[] digest, int number) {
        return (((long) (digest[3 + number * 4] & 0xFF) << 24)
                | ((long) (digest[2 + number * 4] & 0xFF) << 16)
                | ((long) (digest[1 + number * 4] & 0xFF) << 8)
                | (digest[number * 4] & 0xFF))
                & 0xFFFFFFFFL;
    }
}

