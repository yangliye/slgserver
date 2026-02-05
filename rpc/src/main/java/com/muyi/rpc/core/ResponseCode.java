package com.muyi.rpc.core;

/**
 * RPC响应状态码枚举
 *
 * @author muyi
 */
public enum ResponseCode {
    /** 成功 */
    SUCCESS(200),
    /** 失败 */
    FAIL(400),
    /** 错误 */
    ERROR(500),
    /** 超时 */
    TIMEOUT(504),
    /** 服务未找到 */
    SERVICE_NOT_FOUND(404);
    
    private final int code;
    
    ResponseCode(int code) {
        this.code = code;
    }
    
    public int getCode() {
        return code;
    }
}

