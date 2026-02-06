package com.muyi.gate.migrate;

/**
 * 迁移类型
 *
 * @author muyi
 */
public enum MigrationType {
    
    /**
     * World 迁移（最常见）
     * 只切换 World 服务器，Game 数据不变
     * 场景：跨区域、跨服战等
     */
    WORLD,
    
    /**
     * Game 迁移（少见）
     * 切换 Game 服务器，通常 World 也要一起换
     * 场景：合服、服务器负载均衡等
     */
    GAME,
    
    /**
     * 完整迁移
     * Game 和 World 都切换
     * 场景：合服后完整迁移
     */
    FULL
}
