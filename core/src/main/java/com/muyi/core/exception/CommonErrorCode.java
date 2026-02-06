package com.muyi.core.exception;

/**
 * 通用错误码
 * 
 * 错误码规范：
 * - 0: 成功
 * - 1-999: 系统级错误
 * - 1000-1999: 登录模块
 * - 2000-2999: 网关模块
 * - 3000-3999: 游戏模块
 * - 4000-4999: 世界模块
 * - 5000-5999: 联盟模块
 *
 * @author muyi
 */
public enum CommonErrorCode implements ErrorCode {
    
    // ==================== 成功 ====================
    SUCCESS(0, "成功"),
    
    // ==================== 系统级错误 1-999 ====================
    SYSTEM_ERROR(1, "系统错误"),
    PARAM_ERROR(2, "参数错误"),
    UNAUTHORIZED(3, "未授权"),
    FORBIDDEN(4, "禁止访问"),
    NOT_FOUND(5, "资源不存在"),
    TIMEOUT(6, "请求超时"),
    RATE_LIMIT(7, "请求过于频繁"),
    SERVICE_UNAVAILABLE(8, "服务不可用"),
    
    // ==================== 通用业务错误 100-999 ====================
    PLAYER_NOT_FOUND(100, "玩家不存在"),
    PLAYER_OFFLINE(101, "玩家不在线"),
    PLAYER_BANNED(102, "玩家已被封禁"),
    
    RESOURCE_NOT_ENOUGH(200, "资源不足"),
    LEVEL_NOT_ENOUGH(201, "等级不足"),
    VIP_NOT_ENOUGH(202, "VIP等级不足"),
    
    OPERATION_FAILED(300, "操作失败"),
    OPERATION_DENIED(301, "操作被拒绝"),
    OPERATION_TIMEOUT(302, "操作超时"),
    
    DATA_ERROR(400, "数据异常"),
    CONFIG_ERROR(401, "配置错误"),
    
    ;
    
    private final int code;
    private final String message;
    
    CommonErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    @Override
    public int getCode() {
        return code;
    }
    
    @Override
    public String getMessage() {
        return message;
    }
}
