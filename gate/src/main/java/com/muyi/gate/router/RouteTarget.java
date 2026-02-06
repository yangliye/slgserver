package com.muyi.gate.router;

/**
 * 路由目标类型
 *
 * @author muyi
 */
public enum RouteTarget {
    
    /** 登录服务器 */
    LOGIN,
    
    /** Game 服务器 */
    GAME,
    
    /** World 服务器 */
    WORLD,
    
    /** Alliance 服务器 */
    ALLIANCE,
    
    /** Gate 本地处理 */
    GATE_LOCAL
}
