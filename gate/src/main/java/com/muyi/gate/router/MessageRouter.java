package com.muyi.gate.router;

import com.muyi.gate.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息路由器
 * 根据协议ID将消息路由到对应的服务器
 * 
 * 核心设计：
 * 1. 根据协议ID确定路由目标（Login/Game/World/Alliance）
 * 2. 根据 Session 中的路由信息确定具体服务器地址
 * 3. 支持动态更新路由地址（跨服时使用）
 *
 * @author muyi
 */
public class MessageRouter {
    
    private static final Logger log = LoggerFactory.getLogger(MessageRouter.class);
    
    /** 路由规则列表 */
    private final List<RouteRule> rules = new ArrayList<>();
    
    /** 协议ID到路由规则的缓存 */
    private final Map<Integer, RouteRule> ruleCache = new ConcurrentHashMap<>();
    
    /** 默认路由目标 */
    private RouteTarget defaultTarget = RouteTarget.GAME;
    
    public MessageRouter() {
        initDefaultRules();
    }
    
    /**
     * 初始化默认路由规则
     * 协议ID范围规划：
     * - 1000-1999: 登录相关
     * - 2000-4999: Game 服务器（个人数据、战斗等）
     * - 5000-5999: World 服务器（世界地图、活动等）
     * - 6000-6999: Alliance 服务器（联盟相关）
     * - 9000-9999: Gate 本地处理（心跳、断线重连等）
     */
    private void initDefaultRules() {
        // 登录相关
        addRule(RouteRule.range(1000, 1999, RouteTarget.LOGIN, false, "登录协议"));
        
        // Game 服务器
        addRule(RouteRule.range(2000, 4999, RouteTarget.GAME, true, "Game服务协议"));
        
        // World 服务器
        addRule(RouteRule.range(5000, 5999, RouteTarget.WORLD, true, "World服务协议"));
        
        // Alliance 服务器
        addRule(RouteRule.range(6000, 6999, RouteTarget.ALLIANCE, true, "Alliance服务协议"));
        
        // Gate 本地处理
        addRule(RouteRule.range(9000, 9999, RouteTarget.GATE_LOCAL, false, "Gate本地协议"));
    }
    
    /**
     * 添加路由规则
     */
    public void addRule(RouteRule rule) {
        rules.add(rule);
        // 预热缓存
        for (int i = rule.getProtoIdStart(); i <= rule.getProtoIdEnd(); i++) {
            ruleCache.put(i, rule);
        }
        log.debug("Added route rule: {}", rule);
    }
    
    /**
     * 路由消息
     * 
     * @param session 会话
     * @param protoId 协议ID
     * @param message 消息内容
     * @return 路由结果
     */
    public RouteResult route(Session session, int protoId, Object message) {
        // 1. 查找路由规则
        RouteRule rule = findRule(protoId);
        if (rule == null) {
            log.warn("No route rule for protoId: {}, using default target: {}", protoId, defaultTarget);
            rule = new RouteRule(protoId, protoId, defaultTarget, true, "default");
        }
        
        // 2. 检查认证
        if (rule.isRequireAuth() && session.getPlayerId() <= 0) {
            return RouteResult.authRequired(protoId, "未登录");
        }
        
        // 3. 检查迁服状态
        if (session.isMigrating() && rule.getTarget() != RouteTarget.GATE_LOCAL) {
            return RouteResult.migrating(protoId, "迁服中，请稍后");
        }
        
        // 4. 确定目标服务器地址
        String targetAddress = resolveTargetAddress(session, rule.getTarget());
        if (targetAddress == null && rule.getTarget() != RouteTarget.GATE_LOCAL) {
            return RouteResult.noTarget(protoId, "目标服务器不可用");
        }
        
        return RouteResult.success(protoId, rule.getTarget(), targetAddress, message);
    }
    
    /**
     * 查找路由规则
     */
    private RouteRule findRule(int protoId) {
        // 先查缓存
        RouteRule cached = ruleCache.get(protoId);
        if (cached != null) {
            return cached;
        }
        
        // 遍历查找
        for (RouteRule rule : rules) {
            if (rule.matches(protoId)) {
                ruleCache.put(protoId, rule);
                return rule;
            }
        }
        
        return null;
    }
    
    /**
     * 解析目标服务器地址
     */
    private String resolveTargetAddress(Session session, RouteTarget target) {
        switch (target) {
            case LOGIN:
                // TODO: 从服务发现获取
                return "127.0.0.1:8001";
                
            case GAME:
                return session.getGameServerAddress();
                
            case WORLD:
                // World 服务通常是全局的，所有服务器共享
                // TODO: 从服务发现获取
                return "127.0.0.1:8004";
                
            case ALLIANCE:
                return session.getAllianceServerAddress();
                
            case GATE_LOCAL:
                return null; // 本地处理
                
            default:
                return null;
        }
    }
    
    /**
     * 设置默认路由目标
     */
    public void setDefaultTarget(RouteTarget defaultTarget) {
        this.defaultTarget = defaultTarget;
    }
    
    /**
     * 获取路由规则数量
     */
    public int getRuleCount() {
        return rules.size();
    }
}
