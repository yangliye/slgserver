package com.muyi.core.config;

import com.muyi.db.config.DbConfig;
import com.muyi.rpc.client.RpcClientConfig;
import com.muyi.rpc.server.RpcServerConfig;
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
    
    /** 数据库配置（基础） */
    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;
    
    /** 数据库完整配置（包含连接池、异步落地等参数） */
    private DbConfig dbConfig;
    
    /** ZooKeeper 会话超时（毫秒） */
    private int zkSessionTimeout = 5000;
    
    /** ZooKeeper 连接超时（毫秒） */
    private int zkConnectionTimeout = 3000;
    
    /** ZooKeeper 重试初始延迟（毫秒） */
    private int zkRetryInitialDelay = 1000;
    
    /** ZooKeeper 最大重试次数 */
    private int zkRetryMaxRetries = 3;
    
    /** ZooKeeper 重试最大延迟（毫秒） */
    private int zkRetryMaxDelay = 5000;
    
    /** RPC 读空闲超时（秒），0 表示使用默认值 */
    private int rpcReaderIdleTimeSeconds;
    
    /** RPC 发送缓冲区（字节），0 表示使用默认值 */
    private int rpcSendBufferSize;
    
    /** RPC 接收缓冲区（字节），0 表示使用默认值 */
    private int rpcReceiveBufferSize;
    
    /** RPC SO_BACKLOG，0 表示使用默认值 */
    private int rpcBacklog;
    
    /** RPC 服务端完整配置 */
    private RpcServerConfig rpcServerConfig;
    
    /** RPC 客户端配置 */
    private RpcClientConfig rpcClientConfig;
    
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
    
    public ModuleConfig dbConfig(DbConfig dbConfig) {
        this.dbConfig = dbConfig;
        return this;
    }
    
    public ModuleConfig zkSessionTimeout(int ms) {
        this.zkSessionTimeout = ms;
        return this;
    }
    
    public ModuleConfig zkConnectionTimeout(int ms) {
        this.zkConnectionTimeout = ms;
        return this;
    }
    
    public ModuleConfig zkRetryInitialDelay(int ms) {
        this.zkRetryInitialDelay = ms;
        return this;
    }
    
    public ModuleConfig zkRetryMaxRetries(int count) {
        this.zkRetryMaxRetries = count;
        return this;
    }
    
    public ModuleConfig zkRetryMaxDelay(int ms) {
        this.zkRetryMaxDelay = ms;
        return this;
    }
    
    public ModuleConfig rpcReaderIdleTimeSeconds(int seconds) {
        this.rpcReaderIdleTimeSeconds = seconds;
        return this;
    }
    
    public ModuleConfig rpcSendBufferSize(int bytes) {
        this.rpcSendBufferSize = bytes;
        return this;
    }
    
    public ModuleConfig rpcReceiveBufferSize(int bytes) {
        this.rpcReceiveBufferSize = bytes;
        return this;
    }
    
    public ModuleConfig rpcBacklog(int backlog) {
        this.rpcBacklog = backlog;
        return this;
    }
    
    public ModuleConfig rpcServerConfig(RpcServerConfig rpcServerConfig) {
        this.rpcServerConfig = rpcServerConfig;
        return this;
    }
    
    public ModuleConfig rpcClientConfig(RpcClientConfig rpcClientConfig) {
        this.rpcClientConfig = rpcClientConfig;
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
    
    public DbConfig getDbConfig() {
        return dbConfig;
    }
    
    public int getZkSessionTimeout() {
        return zkSessionTimeout;
    }
    
    public int getZkConnectionTimeout() {
        return zkConnectionTimeout;
    }
    
    public int getZkRetryInitialDelay() {
        return zkRetryInitialDelay;
    }
    
    public int getZkRetryMaxRetries() {
        return zkRetryMaxRetries;
    }
    
    public int getZkRetryMaxDelay() {
        return zkRetryMaxDelay;
    }
    
    public int getRpcReaderIdleTimeSeconds() {
        return rpcReaderIdleTimeSeconds;
    }
    
    public int getRpcSendBufferSize() {
        return rpcSendBufferSize;
    }
    
    public int getRpcReceiveBufferSize() {
        return rpcReceiveBufferSize;
    }
    
    public int getRpcBacklog() {
        return rpcBacklog;
    }
    
    public RpcServerConfig getRpcServerConfig() {
        return rpcServerConfig;
    }
    
    public RpcClientConfig getRpcClientConfig() {
        return rpcClientConfig;
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
