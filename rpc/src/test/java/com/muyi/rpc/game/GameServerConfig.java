package com.muyi.rpc.game;

/**
 * Game 服务配置
 *
 * @author muyi
 */
public class GameServerConfig {
    
    /** 服务器ID */
    private int serverId = 1;
    
    /** 服务端口 */
    private int port = 19002;
    
    /** ZooKeeper 地址 */
    private String zkAddress = "localhost:2181";
    
    public GameServerConfig serverId(int serverId) {
        this.serverId = serverId;
        return this;
    }
    
    public GameServerConfig port(int port) {
        this.port = port;
        return this;
    }
    
    public GameServerConfig zkAddress(String zkAddress) {
        this.zkAddress = zkAddress;
        return this;
    }
    
    public int getServerId() {
        return serverId;
    }
    
    public int getPort() {
        return port;
    }
    
    public String getZkAddress() {
        return zkAddress;
    }
}
