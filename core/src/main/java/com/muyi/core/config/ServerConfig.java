package com.muyi.core.config;

import com.muyi.rpc.server.RpcServerConfig;
import org.yaml.snakeyaml.Yaml;

import com.muyi.db.config.DbConfig;
import com.muyi.rpc.client.RpcClientConfig;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务器配置
 * 从 YAML 文件加载配置
 * 
 * 启动时加载所有实例，每个实例一个模块
 *
 * @author muyi
 */
public class ServerConfig {
    
    /** Yaml 解析器工厂（Yaml 非线程安全，每次创建新实例） */
    private static Yaml createYaml() {
        return new Yaml();
    }
    
    /** 主机地址 */
    private String host;
    
    /** ZooKeeper 地址 */
    private String zkAddress = "127.0.0.1:2181";
    
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
    
    /** 全局 Redis 地址 */
    private String redisAddress;
    
    /** RPC 共享 worker 线程数，0 表示使用默认值（CPU 核心数） */
    private int rpcWorkerThreads;
    
    /** RPC 协议最大消息长度（字节），0 表示使用默认值 */
    private int rpcMaxFrameLength;
    
    /** RPC 压缩阈值（字节），0 表示使用默认值 */
    private int rpcCompressThreshold;
    
    /** HTTP 连接超时（秒），0 表示使用默认值 */
    private int httpConnectTimeout;
    
    /** HTTP 读取超时（秒），0 表示使用默认值 */
    private int httpReadTimeout;
    
    /** HTTP 写入超时（秒），0 表示使用默认值 */
    private int httpWriteTimeout;
    
    /** RPC 客户端配置 */
    private RpcClientConfig rpcClientConfig = new RpcClientConfig();
    
    /** Redis 连接池配置 */
    private int redisMaxTotal = 64;
    private int redisMaxIdle = 16;
    private int redisMinIdle = 4;
    private int redisMaxWaitSeconds = 3;
    private int redisConnectTimeout = 3000;
    
    /** 实例配置列表 */
    private List<InstanceConfig> instances = new ArrayList<>();
    
    /** 配置模块设置 */
    private String configRoot = "serverconfig/gamedata";
    private String configPackage;
    
    /**
     * 从 YAML 文件加载配置
     */
    public static ServerConfig load(String path) throws Exception {
        try (InputStream input = new FileInputStream(path)) {
            Map<String, Object> data = createYaml().load(input);
            return parse(data);
        }
    }
    
    /**
     * 从类路径加载配置
     */
    public static ServerConfig loadFromClasspath(String resource) throws Exception {
        try (InputStream input = ServerConfig.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalArgumentException("Resource not found: " + resource);
            }
            Map<String, Object> data = createYaml().load(input);
            return parse(data);
        }
    }
    
    /**
     * 解析配置
     */
    @SuppressWarnings("unchecked")
    private static ServerConfig parse(Map<String, Object> data) {
        ServerConfig config = new ServerConfig();
        
        // 服务器基础配置
        Map<String, Object> server = (Map<String, Object>) data.getOrDefault("server", new HashMap<>());
        config.host = (String) server.get("host");
        config.rpcWorkerThreads = ((Number) server.getOrDefault("rpcWorkerThreads", 0)).intValue();
        config.rpcMaxFrameLength = ((Number) server.getOrDefault("rpcMaxFrameLength", 0)).intValue();
        config.rpcCompressThreshold = ((Number) server.getOrDefault("rpcCompressThreshold", 0)).intValue();
        config.httpConnectTimeout = ((Number) server.getOrDefault("httpConnectTimeout", 0)).intValue();
        config.httpReadTimeout = ((Number) server.getOrDefault("httpReadTimeout", 0)).intValue();
        config.httpWriteTimeout = ((Number) server.getOrDefault("httpWriteTimeout", 0)).intValue();
        
        // 基础设施配置
        Map<String, Object> infra = (Map<String, Object>) data.getOrDefault("infrastructure", new HashMap<>());
        config.zkAddress = (String) infra.getOrDefault("zookeeper", "127.0.0.1:2181");
        config.zkSessionTimeout = ((Number) infra.getOrDefault("zkSessionTimeout", 5000)).intValue();
        config.zkConnectionTimeout = ((Number) infra.getOrDefault("zkConnectionTimeout", 3000)).intValue();
        config.zkRetryInitialDelay = ((Number) infra.getOrDefault("zkRetryInitialDelay", 1000)).intValue();
        config.zkRetryMaxRetries = ((Number) infra.getOrDefault("zkRetryMaxRetries", 3)).intValue();
        config.zkRetryMaxDelay = ((Number) infra.getOrDefault("zkRetryMaxDelay", 5000)).intValue();
        config.redisAddress = (String) infra.get("redis");
        
        // RPC 客户端全局配置
        Map<String, Object> rpcClientData = (Map<String, Object>) data.get("rpcClient");
        if (rpcClientData != null) {
            config.rpcClientConfig = parseRpcClientConfig(rpcClientData);
        }
        
        // 实例配置
        List<Map<String, Object>> instancesData = (List<Map<String, Object>>) data.get("instances");
        if (instancesData != null) {
            for (Map<String, Object> instData : instancesData) {
                InstanceConfig inst = new InstanceConfig();
                inst.module = (String) instData.get("module");
                inst.serverId = ((Number) instData.getOrDefault("serverId", 1)).intValue();
                inst.rpcPort = ((Number) instData.getOrDefault("rpcPort", 0)).intValue();
                inst.webPort = ((Number) instData.getOrDefault("webPort", 0)).intValue();
                
                // Redis 配置
                inst.redisAddress = (String) instData.get("redis");
                
                // 数据库配置
                Map<String, Object> dbData = (Map<String, Object>) instData.get("database");
                if (dbData != null) {
                    inst.jdbcUrl = (String) dbData.get("url");
                    inst.jdbcUser = (String) dbData.get("user");
                    inst.jdbcPassword = (String) dbData.get("password");
                    inst.dbConfig = parseDbConfig(dbData);
                }
                
                // RPC 配置
                Map<String, Object> rpcData = (Map<String, Object>) instData.get("rpc");
                if (rpcData != null) {
                    inst.rpcReaderIdleTimeSeconds = ((Number) rpcData.getOrDefault("readerIdleTimeSeconds", 0)).intValue();
                    inst.rpcSendBufferSize = ((Number) rpcData.getOrDefault("sendBufferSize", 0)).intValue();
                    inst.rpcReceiveBufferSize = ((Number) rpcData.getOrDefault("receiveBufferSize", 0)).intValue();
                    inst.rpcBacklog = ((Number) rpcData.getOrDefault("backlog", 0)).intValue();
                    inst.rpcServerConfig = parseRpcServerConfig(rpcData);
                }
                
                // RPC 客户端配置（实例级）
                Map<String, Object> instRpcClientData = (Map<String, Object>) instData.get("rpcClient");
                if (instRpcClientData != null) {
                    inst.rpcClientConfig = parseRpcClientConfig(instRpcClientData);
                }
                
                // 扩展配置
                Map<String, Object> extraData = (Map<String, Object>) instData.get("extra");
                if (extraData != null) {
                    inst.extra.putAll(extraData);
                }
                
                config.instances.add(inst);
            }
        }
        
        // Redis 全局配置
        Map<String, Object> redisData = (Map<String, Object>) data.get("redis");
        if (redisData != null) {
            config.redisMaxTotal = ((Number) redisData.getOrDefault("maxTotal", 64)).intValue();
            config.redisMaxIdle = ((Number) redisData.getOrDefault("maxIdle", 16)).intValue();
            config.redisMinIdle = ((Number) redisData.getOrDefault("minIdle", 4)).intValue();
            config.redisMaxWaitSeconds = ((Number) redisData.getOrDefault("maxWaitSeconds", 3)).intValue();
            config.redisConnectTimeout = ((Number) redisData.getOrDefault("connectTimeout", 3000)).intValue();
        }
        
        // 配置模块设置
        Map<String, Object> configSection = (Map<String, Object>) data.getOrDefault("config", new HashMap<>());
        config.configRoot = (String) configSection.getOrDefault("configRoot", "serverconfig/gamedata");
        config.configPackage = (String) configSection.get("configPackage");
        
        return config;
    }
    
    /**
     * 解析数据库扩展配置
     */
    private static DbConfig parseDbConfig(Map<String, Object> dbData) {
        DbConfig db = new DbConfig();
        if (dbData.containsKey("url")) db.jdbcUrl((String) dbData.get("url"));
        if (dbData.containsKey("user")) db.username((String) dbData.get("user"));
        if (dbData.containsKey("password")) db.password((String) dbData.get("password"));
        if (dbData.containsKey("maximumPoolSize")) db.maximumPoolSize(((Number) dbData.get("maximumPoolSize")).intValue());
        if (dbData.containsKey("minimumIdle")) db.minimumIdle(((Number) dbData.get("minimumIdle")).intValue());
        if (dbData.containsKey("connectionTimeout")) db.connectionTimeout(((Number) dbData.get("connectionTimeout")).longValue());
        if (dbData.containsKey("idleTimeout")) db.idleTimeout(((Number) dbData.get("idleTimeout")).longValue());
        if (dbData.containsKey("maxLifetime")) db.maxLifetime(((Number) dbData.get("maxLifetime")).longValue());
        if (dbData.containsKey("landThreads")) db.landThreads(((Number) dbData.get("landThreads")).intValue());
        if (dbData.containsKey("landIntervalMs")) db.landIntervalMs(((Number) dbData.get("landIntervalMs")).longValue());
        if (dbData.containsKey("landBatchSize")) db.landBatchSize(((Number) dbData.get("landBatchSize")).intValue());
        if (dbData.containsKey("landMaxRetries")) db.landMaxRetries(((Number) dbData.get("landMaxRetries")).intValue());
        if (dbData.containsKey("prepStmtCacheSize")) db.prepStmtCacheSize(((Number) dbData.get("prepStmtCacheSize")).intValue());
        if (dbData.containsKey("prepStmtCacheSqlLimit")) db.prepStmtCacheSqlLimit(((Number) dbData.get("prepStmtCacheSqlLimit")).intValue());
        if (dbData.containsKey("logSql")) db.logSql((Boolean) dbData.get("logSql"));
        return db;
    }
    
    /**
     * 解析 RPC 服务端配置
     */
    private static com.muyi.rpc.server.RpcServerConfig parseRpcServerConfig(Map<String, Object> rpcData) {
        com.muyi.rpc.server.RpcServerConfig cfg = new com.muyi.rpc.server.RpcServerConfig();
        if (rpcData.containsKey("backlog")) cfg.backlog(((Number) rpcData.get("backlog")).intValue());
        if (rpcData.containsKey("readerIdleTimeSeconds")) cfg.readerIdleTimeSeconds(((Number) rpcData.get("readerIdleTimeSeconds")).intValue());
        if (rpcData.containsKey("sendBufferSize")) cfg.sendBufferSize(((Number) rpcData.get("sendBufferSize")).intValue());
        if (rpcData.containsKey("receiveBufferSize")) cfg.receiveBufferSize(((Number) rpcData.get("receiveBufferSize")).intValue());
        if (rpcData.containsKey("writeLowWaterMark")) cfg.writeLowWaterMark(((Number) rpcData.get("writeLowWaterMark")).intValue());
        if (rpcData.containsKey("writeHighWaterMark")) cfg.writeHighWaterMark(((Number) rpcData.get("writeHighWaterMark")).intValue());
        if (rpcData.containsKey("tcpNoDelay")) cfg.tcpNoDelay((Boolean) rpcData.get("tcpNoDelay"));
        if (rpcData.containsKey("keepAlive")) cfg.keepAlive((Boolean) rpcData.get("keepAlive"));
        if (rpcData.containsKey("reuseAddress")) cfg.reuseAddress((Boolean) rpcData.get("reuseAddress"));
        if (rpcData.containsKey("shutdownTimeoutSeconds")) cfg.shutdownTimeoutSeconds(((Number) rpcData.get("shutdownTimeoutSeconds")).intValue());
        return cfg;
    }
    
    /**
     * 解析 RPC 客户端配置
     */
    private static RpcClientConfig parseRpcClientConfig(Map<String, Object> data) {
        RpcClientConfig cc = new RpcClientConfig();
        if (data.containsKey("connectTimeout")) cc.connectTimeout(((Number) data.get("connectTimeout")).intValue());
        if (data.containsKey("requestTimeout")) cc.requestTimeout(((Number) data.get("requestTimeout")).longValue());
        if (data.containsKey("heartbeatInterval")) cc.heartbeatInterval(((Number) data.get("heartbeatInterval")).intValue());
        if (data.containsKey("heartbeatMaxFailCount")) cc.heartbeatMaxFailCount(((Number) data.get("heartbeatMaxFailCount")).intValue());
        if (data.containsKey("maxConnectionsPerAddress")) cc.maxConnectionsPerAddress(((Number) data.get("maxConnectionsPerAddress")).intValue());
        if (data.containsKey("poolInitialConnections")) cc.poolInitialConnections(((Number) data.get("poolInitialConnections")).intValue());
        if (data.containsKey("retries")) cc.retries(((Number) data.get("retries")).intValue());
        if (data.containsKey("sendBufferSize")) cc.sendBufferSize(((Number) data.get("sendBufferSize")).intValue());
        if (data.containsKey("receiveBufferSize")) cc.receiveBufferSize(((Number) data.get("receiveBufferSize")).intValue());
        if (data.containsKey("writeLowWaterMark")) cc.writeLowWaterMark(((Number) data.get("writeLowWaterMark")).intValue());
        if (data.containsKey("writeHighWaterMark")) cc.writeHighWaterMark(((Number) data.get("writeHighWaterMark")).intValue());
        if (data.containsKey("wheelTimerTickMs")) cc.wheelTimerTickMs(((Number) data.get("wheelTimerTickMs")).intValue());
        if (data.containsKey("wheelTimerTicks")) cc.wheelTimerTicks(((Number) data.get("wheelTimerTicks")).intValue());
        if (data.containsKey("shutdownTimeoutSeconds")) cc.shutdownTimeoutSeconds(((Number) data.get("shutdownTimeoutSeconds")).intValue());
        if (data.containsKey("retryInitialDelayMs")) cc.retryInitialDelayMs(((Number) data.get("retryInitialDelayMs")).longValue());
        if (data.containsKey("retryMaxDelayMs")) cc.retryMaxDelayMs(((Number) data.get("retryMaxDelayMs")).longValue());
        return cc;
    }
    
    /**
     * 根据实例配置生成模块配置
     */
    public ModuleConfig getModuleConfig(InstanceConfig instance) {
        ModuleConfig config = new ModuleConfig()
                .serverId(instance.serverId)
                .host(host)
                .rpcPort(instance.rpcPort)
                .webPort(instance.webPort)
                .zkAddress(zkAddress)
                .zkSessionTimeout(zkSessionTimeout)
                .zkConnectionTimeout(zkConnectionTimeout)
                .zkRetryInitialDelay(zkRetryInitialDelay)
                .zkRetryMaxRetries(zkRetryMaxRetries)
                .zkRetryMaxDelay(zkRetryMaxDelay)
                .redisAddress(instance.redisAddress)
                .jdbcUrl(instance.jdbcUrl)
                .jdbcUser(instance.jdbcUser)
                .jdbcPassword(instance.jdbcPassword)
                .dbConfig(instance.dbConfig)
                .rpcReaderIdleTimeSeconds(instance.rpcReaderIdleTimeSeconds)
                .rpcSendBufferSize(instance.rpcSendBufferSize)
                .rpcReceiveBufferSize(instance.rpcReceiveBufferSize)
                .rpcBacklog(instance.rpcBacklog)
                .rpcServerConfig(instance.rpcServerConfig)
                .rpcClientConfig(instance.rpcClientConfig != null ? instance.rpcClientConfig : rpcClientConfig)
                .extras(instance.extra);
        
        // config 模块的特殊配置
        if ("config".equals(instance.module)) {
            config.extra("configRoot", configRoot);
            config.extra("configPackage", configPackage);
        }
        
        return config;
    }
    
    // ==================== Getter/Setter ====================
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public String getZkAddress() {
        return zkAddress;
    }
    
    public void setZkAddress(String zkAddress) {
        this.zkAddress = zkAddress;
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
    
    public int getRedisMaxTotal() { return redisMaxTotal; }
    public int getRedisMaxIdle() { return redisMaxIdle; }
    public int getRedisMinIdle() { return redisMinIdle; }
    public int getRedisMaxWaitSeconds() { return redisMaxWaitSeconds; }
    public int getRedisConnectTimeout() { return redisConnectTimeout; }
    
    public String getRedisAddress() {
        return redisAddress;
    }
    
    public void setRedisAddress(String redisAddress) {
        this.redisAddress = redisAddress;
    }
    
    public String getConfigRoot() {
        return configRoot;
    }
    
    public void setConfigRoot(String configRoot) {
        this.configRoot = configRoot;
    }
    
    public String getConfigPackage() {
        return configPackage;
    }
    
    public void setConfigPackage(String configPackage) {
        this.configPackage = configPackage;
    }
    
    public int getRpcWorkerThreads() {
        return rpcWorkerThreads;
    }
    
    public void setRpcWorkerThreads(int rpcWorkerThreads) {
        this.rpcWorkerThreads = rpcWorkerThreads;
    }
    
    public int getRpcMaxFrameLength() {
        return rpcMaxFrameLength;
    }
    
    public int getRpcCompressThreshold() {
        return rpcCompressThreshold;
    }
    
    public int getHttpConnectTimeout() {
        return httpConnectTimeout;
    }
    
    public int getHttpReadTimeout() {
        return httpReadTimeout;
    }
    
    public int getHttpWriteTimeout() {
        return httpWriteTimeout;
    }
    
    public RpcClientConfig getRpcClientConfig() {
        return rpcClientConfig;
    }
    
    public List<InstanceConfig> getInstances() {
        return instances;
    }
    
    /**
     * 实例配置
     */
    public static class InstanceConfig {
        /** 模块名称 */
        public String module;
        
        /** 服务器ID */
        public int serverId;
        
        /** RPC 端口 */
        public int rpcPort;
        
        /** Web 端口 */
        public int webPort;
        
        /** Redis 地址（host:port 或 host:port:password） */
        public String redisAddress;
        
        /** 数据库配置 */
        public String jdbcUrl;
        public String jdbcUser;
        public String jdbcPassword;
        
        /** 数据库完整配置 */
        public DbConfig dbConfig;
        
        /** RPC 服务端配置 */
        public int rpcReaderIdleTimeSeconds;
        public int rpcSendBufferSize;
        public int rpcReceiveBufferSize;
        public int rpcBacklog;
        
        /** RPC 服务端完整配置 */
        public RpcServerConfig rpcServerConfig;
        
        /** RPC 客户端配置（实例级，为 null 时使用全局） */
        public RpcClientConfig rpcClientConfig;
        
        /** 扩展配置 */
        public Map<String, Object> extra = new HashMap<>();
        
        public InstanceConfig() {
        }
        
        /**
         * 获取实例唯一标识
         */
        public String getInstanceId() {
            return module + "-" + serverId;
        }
        
        @Override
        public String toString() {
            return module + "-" + serverId + "(rpc=" + rpcPort + ", web=" + webPort + ")";
        }
    }
}
