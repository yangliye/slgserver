package com.muyi.core.config;

import java.util.HashMap;
import java.util.Map;

/**
 * 模块配置
 *
 * @author muyi
 */
public class ModuleConfig {
    
    /** 服务器ID */
    private int serverId;
    
    /** 主机地址（用于 RPC 注册） */
    private String host;
    
    /** RPC 端口 */
    private int rpcPort;
    
    /** Web 端口 */
    private int webPort;
    
    /** ZooKeeper 地址 */
    private String zkAddress;
    
    /** Redis 地址 */
    private String redisAddress;
    
    /** 数据库配置 */
    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;
    
    /** 扩展配置 */
    private Map<String, Object> extra = new HashMap<>();
    
    // ==================== Builder 方法 ====================
    
    public ModuleConfig serverId(int serverId) {
        this.serverId = serverId;
        return this;
    }
    
    public ModuleConfig host(String host) {
        this.host = host;
        return this;
    }
    
    public ModuleConfig rpcPort(int rpcPort) {
        this.rpcPort = rpcPort;
        return this;
    }
    
    public ModuleConfig webPort(int webPort) {
        this.webPort = webPort;
        return this;
    }
    
    public ModuleConfig zkAddress(String zkAddress) {
        this.zkAddress = zkAddress;
        return this;
    }
    
    public ModuleConfig redisAddress(String redisAddress) {
        this.redisAddress = redisAddress;
        return this;
    }
    
    public ModuleConfig jdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
        return this;
    }
    
    public ModuleConfig jdbcUser(String jdbcUser) {
        this.jdbcUser = jdbcUser;
        return this;
    }
    
    public ModuleConfig jdbcPassword(String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
        return this;
    }
    
    public ModuleConfig extra(String key, Object value) {
        this.extra.put(key, value);
        return this;
    }
    
    public ModuleConfig extras(Map<String, Object> extras) {
        if (extras != null) {
            this.extra.putAll(extras);
        }
        return this;
    }
    
    // ==================== Getter ====================
    
    public int getServerId() {
        return serverId;
    }
    
    public String getHost() {
        return host;
    }
    
    public int getRpcPort() {
        return rpcPort;
    }
    
    public int getWebPort() {
        return webPort;
    }
    
    public String getZkAddress() {
        return zkAddress;
    }
    
    public String getRedisAddress() {
        return redisAddress;
    }
    
    public String getJdbcUrl() {
        return jdbcUrl;
    }
    
    public String getJdbcUser() {
        return jdbcUser;
    }
    
    public String getJdbcPassword() {
        return jdbcPassword;
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getExtra(String key) {
        return (T) extra.get(key);
    }
    
    public <T> T getExtra(String key, T defaultValue) {
        Object value = extra.get(key);
        if (value == null) {
            return defaultValue;
        }
        return (T) value;
    }
    
    public Map<String, Object> getExtras() {
        return extra;
    }
}
