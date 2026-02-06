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
 * 支持三种启动模式：
 * - all: 全部模块各启动一个实例
 * - single: 只启动指定的单个模块
 * - instance: 按 instances 配置启动多实例
 *
 * @author muyi
 */
public class ServerConfig {
    
    /** Yaml 解析器工厂（Yaml 非线程安全，每次创建新实例） */
    private static Yaml createYaml() {
        return new Yaml();
    }
    
    /** 启动模式：all=全部模块, single=单模块, instance=多实例 */
    private String mode = "all";
    
    /** 服务器ID */
    private int serverId = 1;
    
    /** 主机地址 */
    private String host;
    
    /** 要启动的模块列表 */
    private List<String> modules = new ArrayList<>();
    
    /** ZooKeeper 地址 */
    private String zkAddress = "127.0.0.1:2181";
    
    /** Redis 地址 */
    private String redisAddress = "127.0.0.1:6379";
    
    /** 数据库配置 */
    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;
    
    /** 各模块端口配置 */
    private Map<String, PortConfig> ports = new HashMap<>();
    
    /** 多实例配置列表 */
    private List<InstanceConfig> instances = new ArrayList<>();
    
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
        
        Map<String, Object> server = (Map<String, Object>) data.getOrDefault("server", new HashMap<>());
        
        config.mode = (String) server.getOrDefault("mode", "all");
        config.serverId = ((Number) server.getOrDefault("serverId", 1)).intValue();
        config.host = (String) server.get("host");
        
        Object modulesObj = server.get("modules");
        if (modulesObj instanceof List) {
            config.modules = (List<String>) modulesObj;
        }
        
        // 基础设施配置
        Map<String, Object> infra = (Map<String, Object>) data.getOrDefault("infrastructure", new HashMap<>());
        config.zkAddress = (String) infra.getOrDefault("zookeeper", "127.0.0.1:2181");
        config.redisAddress = (String) infra.getOrDefault("redis", "127.0.0.1:6379");
        
        // 数据库配置
        Map<String, Object> db = (Map<String, Object>) data.getOrDefault("database", new HashMap<>());
        config.jdbcUrl = (String) db.get("url");
        config.jdbcUser = (String) db.get("user");
        config.jdbcPassword = (String) db.get("password");
        
        // 端口配置
        Map<String, Object> portsData = (Map<String, Object>) data.getOrDefault("ports", new HashMap<>());
        for (Map.Entry<String, Object> entry : portsData.entrySet()) {
            Map<String, Object> portData = (Map<String, Object>) entry.getValue();
            PortConfig portConfig = new PortConfig();
            portConfig.rpcPort = ((Number) portData.getOrDefault("rpc", 0)).intValue();
            portConfig.webPort = ((Number) portData.getOrDefault("web", 0)).intValue();
            config.ports.put(entry.getKey(), portConfig);
        }
        
        // 默认端口配置
        config.ports.putIfAbsent("login", new PortConfig(10001, 18001));
        config.ports.putIfAbsent("gate", new PortConfig(10002, 18002));
        config.ports.putIfAbsent("game", new PortConfig(10003, 18003));
        config.ports.putIfAbsent("world", new PortConfig(10004, 18004));
        config.ports.putIfAbsent("alliance", new PortConfig(10005, 18005));
        
        // 多实例配置
        List<Map<String, Object>> instancesData = (List<Map<String, Object>>) data.get("instances");
        if (instancesData != null) {
            for (Map<String, Object> instData : instancesData) {
                InstanceConfig inst = new InstanceConfig();
                inst.name = (String) instData.get("name");
                inst.module = (String) instData.get("module");
                inst.serverId = ((Number) instData.getOrDefault("serverId", 1)).intValue();
                inst.rpcPort = ((Number) instData.getOrDefault("rpcPort", 0)).intValue();
                inst.webPort = ((Number) instData.getOrDefault("webPort", 0)).intValue();
                
                Map<String, Object> extraData = (Map<String, Object>) instData.get("extra");
                if (extraData != null) {
                    inst.extra.putAll(extraData);
                }
                
                config.instances.add(inst);
            }
        }
        
        return config;
    }
    
    /**
     * 获取模块配置
     */
    public ModuleConfig getModuleConfig(String moduleName) {
        PortConfig portConfig = ports.getOrDefault(moduleName, new PortConfig());
        
        return new ModuleConfig()
                .serverId(serverId)
                .host(host)
                .rpcPort(portConfig.rpcPort)
                .webPort(portConfig.webPort)
                .zkAddress(zkAddress)
                .redisAddress(redisAddress)
                .jdbcUrl(jdbcUrl)
                .jdbcUser(jdbcUser)
                .jdbcPassword(jdbcPassword);
    }
    
    /**
     * 是否启用全部模块
     */
    public boolean isAllModules() {
        return "all".equalsIgnoreCase(mode);
    }
    
    /**
     * 是否是多实例模式
     */
    public boolean isInstanceMode() {
        return "instance".equalsIgnoreCase(mode);
    }
    
    /**
     * 获取实例配置列表
     */
    public List<InstanceConfig> getInstances() {
        return instances;
    }
    
    /**
     * 根据实例配置生成 ModuleConfig
     */
    public ModuleConfig getModuleConfig(InstanceConfig instance) {
        return new ModuleConfig()
                .serverId(instance.serverId)
                .host(host)
                .rpcPort(instance.rpcPort)
                .webPort(instance.webPort)
                .zkAddress(zkAddress)
                .redisAddress(redisAddress)
                .jdbcUrl(jdbcUrl)
                .jdbcUser(jdbcUser)
                .jdbcPassword(jdbcPassword)
                .extra("instanceName", instance.name)
                .extras(instance.extra);
    }
    
    // ==================== Getter/Setter ====================
    
    public String getMode() {
        return mode;
    }
    
    public void setMode(String mode) {
        this.mode = mode;
    }
    
    public int getServerId() {
        return serverId;
    }
    
    public void setServerId(int serverId) {
        this.serverId = serverId;
    }
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public List<String> getModules() {
        return modules;
    }
    
    public void setModules(List<String> modules) {
        this.modules = modules;
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
    
    /**
     * 端口配置
     */
    public static class PortConfig {
        public int rpcPort;
        public int webPort;
        
        public PortConfig() {
        }
        
        public PortConfig(int rpcPort, int webPort) {
            this.rpcPort = rpcPort;
            this.webPort = webPort;
        }
    }
    
    /**
     * 实例配置（多实例模式）
     */
    public static class InstanceConfig {
        /** 实例名称 */
        public String name;
        
        /** 模块类型 */
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
        
        @Override
        public String toString() {
            return "InstanceConfig{" +
                    "name='" + name + '\'' +
                    ", module='" + module + '\'' +
                    ", serverId=" + serverId +
                    ", rpcPort=" + rpcPort +
                    ", webPort=" + webPort +
                    '}';
        }
    }
}
