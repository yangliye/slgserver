package com.muyi.gate.router;

/**
 * 路由规则
 * 定义消息协议到服务器的映射规则
 *
 * @author muyi
 */
public class RouteRule {
    
    /** 协议ID范围起始 */
    private final int protoIdStart;
    
    /** 协议ID范围结束 */
    private final int protoIdEnd;
    
    /** 路由目标 */
    private final RouteTarget target;
    
    /** 是否需要登录 */
    private final boolean requireAuth;
    
    /** 描述 */
    private final String description;
    
    public RouteRule(int protoIdStart, int protoIdEnd, RouteTarget target, boolean requireAuth, String description) {
        this.protoIdStart = protoIdStart;
        this.protoIdEnd = protoIdEnd;
        this.target = target;
        this.requireAuth = requireAuth;
        this.description = description;
    }
    
    /**
     * 单协议规则
     */
    public static RouteRule of(int protoId, RouteTarget target, boolean requireAuth, String description) {
        return new RouteRule(protoId, protoId, target, requireAuth, description);
    }
    
    /**
     * 范围规则
     */
    public static RouteRule range(int start, int end, RouteTarget target, boolean requireAuth, String description) {
        return new RouteRule(start, end, target, requireAuth, description);
    }
    
    /**
     * 是否匹配
     */
    public boolean matches(int protoId) {
        return protoId >= protoIdStart && protoId <= protoIdEnd;
    }
    
    public int getProtoIdStart() {
        return protoIdStart;
    }
    
    public int getProtoIdEnd() {
        return protoIdEnd;
    }
    
    public RouteTarget getTarget() {
        return target;
    }
    
    public boolean isRequireAuth() {
        return requireAuth;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        if (protoIdStart == protoIdEnd) {
            return String.format("RouteRule[%d -> %s, auth=%b] %s",
                    protoIdStart, target, requireAuth, description);
        }
        return String.format("RouteRule[%d-%d -> %s, auth=%b] %s",
                protoIdStart, protoIdEnd, target, requireAuth, description);
    }
}
