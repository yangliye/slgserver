package com.muyi.rpc.core;

import com.muyi.common.util.time.TimeUtils;

import java.io.Serializable;

/**
 * RPC响应对象
 *
 * @author muyi
 */
public class RpcResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 请求ID（与请求对应） */
    private long requestId;
    
    /** 响应状态码 */
    private int code;
    
    /** 错误信息 */
    private String message;
    
    /** 返回数据 */
    private Object data;
    
    /** 异常类名（避免直接序列化 Throwable，可能导致序列化问题或泄露敏感信息） */
    private String exceptionClass;
    
    /** 异常堆栈信息（可选，用于调试） */
    private String exceptionStack;
    
    /** 响应时间戳 */
    private long timestamp;
    
    public RpcResponse() {
        this.timestamp = TimeUtils.currentTimeMillis();
    }
    
    /**
     * 创建成功响应
     */
    public static RpcResponse success(long requestId, Object data) {
        RpcResponse response = new RpcResponse();
        response.setRequestId(requestId);
        response.setCode(ResponseCode.SUCCESS.getCode());
        response.setData(data);
        return response;
    }
    
    /**
     * 创建失败响应
     */
    public static RpcResponse fail(long requestId, String message) {
        RpcResponse response = new RpcResponse();
        response.setRequestId(requestId);
        response.setCode(ResponseCode.FAIL.getCode());
        response.setMessage(message);
        return response;
    }
    
    /**
     * 创建异常响应
     */
    public static RpcResponse error(long requestId, Throwable exception) {
        RpcResponse response = new RpcResponse();
        response.setRequestId(requestId);
        response.setCode(ResponseCode.ERROR.getCode());
        response.setMessage(exception.getMessage());
        response.setExceptionClass(exception.getClass().getName());
        // 保存堆栈信息用于调试（可考虑通过配置开关控制）
        response.setExceptionStack(getStackTraceAsString(exception));
        return response;
    }
    
    /**
     * 将异常堆栈转为字符串
     */
    private static String getStackTraceAsString(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        java.io.StringWriter sw = new java.io.StringWriter();
        throwable.printStackTrace(new java.io.PrintWriter(sw));
        String stackTrace = sw.toString();
        // 限制堆栈长度，避免数据过大
        if (stackTrace.length() > 4096) {
            stackTrace = stackTrace.substring(0, 4096) + "\n... (truncated)";
        }
        return stackTrace;
    }
    
    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return code == ResponseCode.SUCCESS.getCode();
    }
    
    // Getters and Setters
    
    public long getRequestId() {
        return requestId;
    }
    
    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }
    
    public int getCode() {
        return code;
    }
    
    public void setCode(int code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
    
    public String getExceptionClass() {
        return exceptionClass;
    }
    
    public void setExceptionClass(String exceptionClass) {
        this.exceptionClass = exceptionClass;
    }
    
    public String getExceptionStack() {
        return exceptionStack;
    }
    
    public void setExceptionStack(String exceptionStack) {
        this.exceptionStack = exceptionStack;
    }
    
    /**
     * 获取异常（兼容旧代码，根据 exceptionClass 重建异常）
     */
    public Throwable getException() {
        if (exceptionClass == null && message == null) {
            return null;
        }
        String errorMsg = message != null ? message : "RPC call failed";
        // 如果有异常类名，添加前缀；否则直接使用消息
        if (exceptionClass != null && !exceptionClass.isEmpty()) {
            return new RuntimeException("[" + exceptionClass + "] " + errorMsg);
        }
        return new RuntimeException(errorMsg);
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "RpcResponse{" +
                "requestId=" + requestId +
                ", code=" + code +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}
