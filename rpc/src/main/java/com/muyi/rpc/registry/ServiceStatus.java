package com.muyi.rpc.registry;

/**
 * 服务状态枚举
 *
 * @author muyi
 */
public enum ServiceStatus {
    /** 正常 */
    UP,
    /** 下线中（优雅关闭） */
    DRAINING,
    /** 已下线 */
    DOWN,
    /** 不健康 */
    UNHEALTHY
}

