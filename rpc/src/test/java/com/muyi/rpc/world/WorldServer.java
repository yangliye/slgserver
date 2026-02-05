package com.muyi.rpc.world;

import com.muyi.rpc.Rpc;
import com.muyi.rpc.server.RpcServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * World 服务器
 *
 * @author muyi
 */
public class WorldServer {
    
    private static final Logger logger = LoggerFactory.getLogger(WorldServer.class);
    
    private final WorldServerConfig config;
    
    private RpcServer rpcServer;
    private WorldServiceImpl worldService;
    
    public WorldServer(WorldServerConfig config) {
        this.config = config;
    }
    
    /**
     * 启动 World 服务
     */
    public void start() throws Exception {
        logger.info("========== Starting World Server [{}] on port {} ==========", 
                config.getServerId(), config.getPort());
        
        worldService = new WorldServiceImpl();
        
        // 使用 Builder 模式注册服务
        rpcServer = Rpc.server(config.getPort())
                .serverId(config.getServerId())
                .zookeeper(config.getZkAddress())
                .register(worldService)
                .start();
        
        logger.info("========== World Server [{}] started ==========", config.getServerId());
    }
    
    /**
     * 关闭服务
     */
    public void shutdown() {
        logger.info("Shutting down World Server [{}]...", config.getServerId());
        if (rpcServer != null) {
            rpcServer.shutdown();
        }
        logger.info("World Server [{}] stopped", config.getServerId());
    }
    
    public WorldServiceImpl getWorldService() {
        return worldService;
    }
    
    public int getServerId() {
        return config.getServerId();
    }
    
    public int getPort() {
        return config.getPort();
    }
}
