package com.muyi.common.exception;

/**
 * 通用错误码
 * 
 * 错误码规范：
 * - 0: 成功
 * - 1-999: 通用错误
 * - 1000-1999: 登录模块
 * - 2000-2999: Gate模块
 * - 3000-3999: Game模块
 * - 4000-4999: World模块
 * - 5000-5999: Alliance模块
 *
 * @author muyi
 */
public enum CommonErrorCode implements ErrorCode {
    
    // ==================== 成功 ====================
    SUCCESS(0, "成功"),
    
    // ==================== 通用错误 1-999 ====================
    UNKNOWN_ERROR(1, "未知错误"),
    SYSTEM_ERROR(2, "系统错误"),
    PARAM_ERROR(3, "参数错误"),
    PARAM_MISSING(4, "参数缺失"),
    PARAM_INVALID(5, "参数无效"),
    
    DATA_NOT_FOUND(10, "数据不存在"),
    DATA_ALREADY_EXISTS(11, "数据已存在"),
    DATA_EXPIRED(12, "数据已过期"),
    
    PERMISSION_DENIED(20, "权限不足"),
    NOT_LOGIN(21, "未登录"),
    TOKEN_INVALID(22, "Token无效"),
    TOKEN_EXPIRED(23, "Token已过期"),
    
    OPERATION_FAILED(30, "操作失败"),
    OPERATION_TOO_FREQUENT(31, "操作过于频繁"),
    OPERATION_NOT_ALLOWED(32, "操作不允许"),
    
    RESOURCE_NOT_ENOUGH(40, "资源不足"),
    LEVEL_NOT_ENOUGH(41, "等级不足"),
    VIP_NOT_ENOUGH(42, "VIP等级不足"),
    
    SERVER_BUSY(50, "服务器繁忙"),
    SERVER_MAINTENANCE(51, "服务器维护中"),
    SERVER_NOT_AVAILABLE(52, "服务不可用"),
    
    NETWORK_ERROR(60, "网络错误"),
    TIMEOUT(61, "请求超时"),
    
    CONFIG_ERROR(70, "配置错误"),
    CONFIG_NOT_FOUND(71, "配置不存在"),
    
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
