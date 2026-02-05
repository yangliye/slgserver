package com.muyi.db.core;

/**
 * 实体状态枚举
 */
public enum EntityState {
    
    /**
     * 新建状态，需要 INSERT
     */
    NEW,
    
    /**
     * 持久化状态，可能需要 UPDATE
     */
    PERSISTENT,
    
    /**
     * 已删除状态，需要 DELETE
     */
    DELETED,
    
    /**
     * 游离状态，与数据库无关
     */
    DETACHED
}
