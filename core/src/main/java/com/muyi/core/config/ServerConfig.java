package com.muyi.core.config;

import org.yaml.snakeyaml.Yaml;

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
    
    /** Redis 地址 */
    private String redisAddress = "127.0.0.1:6379";
    
    /** 数据库配置 */
    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;
    
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
        
        // 基础设施配置
        Map<String, Object> infra = (Map<String, Object>) data.getOrDefault("infrastructure", new HashMap<>());
        config.zkAddress = (String) infra.getOrDefault("zookeeper", "127.0.0.1:2181");
        config.redisAddress = (String) infra.getOrDefault("redis", "127.0.0.1:6379");
        
        // 数据库配置
        Map<String, Object> db = (Map<String, Object>) data.getOrDefault("database", new HashMap<>());
        config.jdbcUrl = (String) db.get("url");
        config.jdbcUser = (String) db.get("user");
        config.jdbcPassword = (String) db.get("password");
        
        // 实例配置
        List<Map<String, Object>> instancesData = (List<Map<String, Object>>) data.get("instances");
        if (instancesData != null) {
            for (Map<String, Object> instData : instancesData) {
                InstanceConfig inst = new InstanceConfig();
                inst.module = (String) instData.get("module");
                inst.serverId = ((Number) instData.getOrDefault("serverId", 1)).intValue();
                inst.rpcPort = ((Number) instData.getOrDefault("rpcPort", 0)).intValue();
                inst.webPort = ((Number) instData.getOrDefault("webPort", 0)).intValue();
                
                // 扩展配置
                Map<String, Object> extraData = (Map<String, Object>) instData.get("extra");
                if (extraData != null) {
                    inst.extra.putAll(extraData);
                }
                
                config.instances.add(inst);
            }
        }
        
        // 配置模块设置
        Map<String, Object> configSection = (Map<String, Object>) data.getOrDefault("config", new HashMap<>());
        config.configRoot = (String) configSection.getOrDefault("configRoot", "serverconfig/gamedata");
        config.configPackage = (String) configSection.get("configPackage");
        
        return config;
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
                .redisAddress(redisAddress)
                .jdbcUrl(jdbcUrl)
                .jdbcUser(jdbcUser)
                .jdbcPassword(jdbcPassword)
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
