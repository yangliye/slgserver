package com.muyi.core.exception;

/**
 * 错误码接口
 * 各业务模块可实现此接口定义自己的错误码
 *
 * @author muyi
 */
public interface ErrorCode {
    
    /**
     * 获取错误码
     */
    int getCode();
    
    /**
     * 获取错误消息
     */
    String getMessage();
}
