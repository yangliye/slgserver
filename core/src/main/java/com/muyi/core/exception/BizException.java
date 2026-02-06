package com.muyi.core.exception;

/**
 * 业务异常
 * 用于业务逻辑中的可预期异常，携带错误码
 *
 * @author muyi
 */
public class BizException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /** 错误码 */
    private final int code;
    
    /** 错误码枚举（可选） */
    private final ErrorCode errorCode;
    
    /**
     * 使用错误码枚举构造
     */
    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }
    
    /**
     * 使用错误码枚举 + 自定义消息
     */
    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }
    
    /**
     * 使用错误码枚举 + 格式化消息
     */
    public BizException(ErrorCode errorCode, String format, Object... args) {
        super(String.format(format, args));
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }
    
    /**
     * 使用自定义错误码
     */
    public BizException(int code, String message) {
        super(message);
        this.code = code;
        this.errorCode = null;
    }
    
    /**
     * 使用自定义错误码 + 原因
     */
    public BizException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.errorCode = null;
    }
    
    /**
     * 使用错误码枚举 + 原因
     */
    public BizException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }
    
    /**
     * 获取错误码
     */
    public int getCode() {
        return code;
    }
    
    /**
     * 获取错误码枚举
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    /**
     * 是否是指定的错误码
     */
    public boolean is(ErrorCode errorCode) {
        return this.code == errorCode.getCode();
    }
    
    // ==================== 静态工厂方法 ====================
    
    /**
     * 快速创建异常
     */
    public static BizException of(ErrorCode errorCode) {
        return new BizException(errorCode);
    }
    
    public static BizException of(ErrorCode errorCode, String message) {
        return new BizException(errorCode, message);
    }
    
    public static BizException of(int code, String message) {
        return new BizException(code, message);
    }
    
    /**
     * 参数错误
     */
    public static BizException paramError(String message) {
        return new BizException(CommonErrorCode.PARAM_ERROR, message);
    }
    
    /**
     * 资源不存在
     */
    public static BizException notFound(String message) {
        return new BizException(CommonErrorCode.NOT_FOUND, message);
    }
    
    /**
     * 操作失败
     */
    public static BizException operationFailed(String message) {
        return new BizException(CommonErrorCode.OPERATION_FAILED, message);
    }
    
    @Override
    public String toString() {
        return "BizException{code=" + code + ", message='" + getMessage() + "'}";
    }
}
