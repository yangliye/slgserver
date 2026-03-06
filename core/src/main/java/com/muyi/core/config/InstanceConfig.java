package com.muyi.core.config;

import com.muyi.db.config.DbConfig;
import com.muyi.rpc.client.RpcClientConfig;
import com.muyi.rpc.server.RpcServerConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * 实例配置
 *
 * @author muyi
 */
public class InstanceConfig {
    
    /** 模块名称 */
    public String module;
    
    /** 服务器ID */
    public int serverId;
    
    /** RPC 端口 */
    public int rpcPort;
    
    /** Web 端口 */
    public int webPort;
    
    /** TCP 端口（Gate 客户端接入） */
    public int tcpPort;
    
    /** 是否启用 Groovy 热执行 */
    public boolean groovyEnabled;
    
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
