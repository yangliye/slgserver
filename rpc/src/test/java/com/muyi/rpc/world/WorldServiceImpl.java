package com.muyi.rpc.world;

import com.muyi.rpc.annotation.RpcService;
import com.muyi.rpc.worldi.IWorldService;
import com.muyi.rpc.worldi.WorldEnterResult;
import com.muyi.rpc.worldi.WorldInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 世界服务实现
 *
 * @author muyi
 */
@RpcService(IWorldService.class)
public class WorldServiceImpl implements IWorldService {
    
    private static final Logger logger = LoggerFactory.getLogger(WorldServiceImpl.class);
    
    /** 世界信息缓存 */
    private final Map<Integer, WorldInfo> worldCache = new ConcurrentHashMap<>();
    
    /** 玩家所在世界 */
    private final Map<Long, Integer> playerWorldMap = new ConcurrentHashMap<>();
    
    /** 世界在线玩家 */
    private final Map<Integer, Set<Long>> worldPlayers = new ConcurrentHashMap<>();
    
    public WorldServiceImpl() {
        initDefaultWorlds();
    }
    
    private void initDefaultWorlds() {
        for (int i = 1; i <= 5; i++) {
            WorldInfo world = new WorldInfo(i, "World-" + i);
            world.setMaxPlayers(1000);
            world.setOnlineCount(0);
            world.setServerAddress("127.0.0.1:1880" + i);
            worldCache.put(i, world);
            worldPlayers.put(i, ConcurrentHashMap.newKeySet());
        }
        logger.info("WorldService initialized with {} worlds", worldCache.size());
    }
    
    @Override
    public WorldEnterResult enterWorld(long playerId, int worldId) {
        logger.info("[World] Player {} entering world {}", playerId, worldId);
        
        WorldInfo world = worldCache.get(worldId);
        if (world == null) {
            return WorldEnterResult.fail("World not found: " + worldId);
        }
        
        if (world.getOnlineCount() >= world.getMaxPlayers()) {
            return WorldEnterResult.fail("World is full");
        }
        
        Integer currentWorld = playerWorldMap.get(playerId);
        if (currentWorld != null && currentWorld != worldId) {
            leaveWorld(playerId);
        }
        
        playerWorldMap.put(playerId, worldId);
        worldPlayers.get(worldId).add(playerId);
        world.setOnlineCount(worldPlayers.get(worldId).size());
        
        logger.info("[World] Player {} entered world {} successfully, online: {}", 
                playerId, worldId, world.getOnlineCount());
        
        return WorldEnterResult.success(world);
    }
    
    @Override
    public WorldInfo getWorldInfo(int worldId) {
        return worldCache.get(worldId);
    }
    
    @Override
    public void leaveWorld(long playerId) {
        Integer worldId = playerWorldMap.remove(playerId);
        if (worldId != null) {
            Set<Long> players = worldPlayers.get(worldId);
            if (players != null) {
                players.remove(playerId);
                WorldInfo world = worldCache.get(worldId);
                if (world != null) {
                    world.setOnlineCount(players.size());
                }
            }
            logger.info("[World] Player {} left world {}", playerId, worldId);
        }
    }
    
    @Override
    public void broadcastToWorld(int worldId, String message) {
        Set<Long> players = worldPlayers.get(worldId);
        if (players != null) {
            logger.info("[World] Broadcasting to world {} ({} players): {}", 
                    worldId, players.size(), message);
        }
    }
}
