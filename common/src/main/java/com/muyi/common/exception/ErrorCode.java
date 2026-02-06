package com.muyi.common.exception;

/**
 * 错误码接口
 * 各模块可以实现此接口定义自己的错误码
 *
 * @author muyi
 */
public interface ErrorCode {
    
    /**
     * 错误码
     */
    int getCode();
    
    /**
     * 错误消息
     */
    String getMessage();
}
