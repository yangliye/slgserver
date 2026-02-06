package com.muyi.gate.session;

/**
 * 会话状态
 *
 * @author muyi
 */
public enum SessionState {
    
    /** 已连接，未登录 */
    CONNECTED,
    
    /** 已认证，等待进入游戏 */
    AUTHENTICATED,
    
    /** 游戏中 */
    GAMING,
    
    /** 迁服中（暂停消息处理） */
    MIGRATING,
    
    /** 离线 */
    OFFLINE
}
