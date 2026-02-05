package com.muyi.rpc.registry;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务实例信息
 * 包含服务地址、权重、元数据等信息
 *
 * @author muyi
 */
public class ServiceInstance implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 服务标识 */
    private String serviceKey;
    
    /** 服务地址 (host:port) */
    private String address;
    
    /** 服务器ID（唯一标识） */
    private String serverId;
    
    /** 服务权重（用于负载均衡）*/
    private int weight = 100;
    
    /** 注册时间戳 */
    private long registerTime;
    
    /** 最后心跳时间 */
    private long lastHeartbeatTime;
    
    /** 服务状态 */
    private ServiceStatus status = ServiceStatus.UP;
    
    /** 扩展元数据（线程安全） */
    private Map<String, String> metadata = new ConcurrentHashMap<>();
    
    public ServiceInstance() {
        this.registerTime = System.currentTimeMillis();
        this.lastHeartbeatTime = this.registerTime;
    }
    
    public ServiceInstance(String serviceKey, String address) {
        this();
        this.serviceKey = serviceKey;
        this.address = address;
    }
    
    // ========== 元数据操作 ==========
    
    public ServiceInstance addMetadata(String key, String value) {
        this.metadata.put(key, value);
        return this;
    }
    
    public String getMetadata(String key) {
        return metadata.get(key);
    }
    
    public String getMetadata(String key, String defaultValue) {
        return metadata.getOrDefault(key, defaultValue);
    }
    
    // ========== 常用元数据Key ==========
    
    /** 区服ID */
    public static final String META_ZONE_ID = "zoneId";
    /** 服务器负载 */
    public static final String META_LOAD = "load";
    /** 最大连接数 */
    public static final String META_MAX_CONNECTIONS = "maxConnections";
    /** 当前连接数 */
    public static final String META_CURRENT_CONNECTIONS = "currentConnections";
    /** 版本号 */
    public static final String META_VERSION = "version";
    
    // ========== Getters and Setters ==========
    
    public String getServiceKey() {
        return serviceKey;
    }
    
    public void setServiceKey(String serviceKey) {
        this.serviceKey = serviceKey;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getServerId() {
        return serverId;
    }
    
    public void setServerId(String serverId) {
        this.serverId = serverId;
    }
    
    public int getWeight() {
        return weight;
    }
    
    public void setWeight(int weight) {
        this.weight = weight;
    }
    
    public long getRegisterTime() {
        return registerTime;
    }
    
    public void setRegisterTime(long registerTime) {
        this.registerTime = registerTime;
    }
    
    public long getLastHeartbeatTime() {
        return lastHeartbeatTime;
    }
    
    public void setLastHeartbeatTime(long lastHeartbeatTime) {
        this.lastHeartbeatTime = lastHeartbeatTime;
    }
    
    public ServiceStatus getStatus() {
        return status;
    }
    
    public void setStatus(ServiceStatus status) {
        this.status = status;
    }
    
    public Map<String, String> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, String> metadata) {
        // 确保线程安全，将传入的 Map 转换为 ConcurrentHashMap
        if (metadata instanceof ConcurrentHashMap) {
            this.metadata = metadata;
        } else if (metadata != null) {
            this.metadata = new ConcurrentHashMap<>(metadata);
        } else {
            this.metadata = new ConcurrentHashMap<>();
        }
    }
    
    @Override
    public String toString() {
        return "ServiceInstance{" +
                "serviceKey='" + serviceKey + '\'' +
                ", address='" + address + '\'' +
                ", serverId='" + serverId + '\'' +
                ", weight=" + weight +
                ", status=" + status +
                '}';
    }
}
