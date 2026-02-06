package com.muyi.common.exception;

/**
 * 游戏业务异常
 * 所有业务逻辑错误都应该抛出此异常
 *
 * @author muyi
 */
public class GameException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /** 错误码 */
    private final int code;
    
    /** 错误消息 */
    private final String msg;
    
    /** 附加数据 */
    private Object data;
    
    /**
     * 使用错误码枚举构造
     */
    public GameException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.msg = errorCode.getMessage();
    }
    
    /**
     * 使用错误码枚举 + 自定义消息
     */
    public GameException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.msg = message;
    }
    
    /**
     * 使用错误码枚举 + 格式化消息
     */
    public GameException(ErrorCode errorCode, String format, Object... args) {
        super(String.format(format, args));
        this.code = errorCode.getCode();
        this.msg = String.format(format, args);
    }
    
    /**
     * 使用自定义错误码和消息
     */
    public GameException(int code, String message) {
        super(message);
        this.code = code;
        this.msg = message;
    }
    
    /**
     * 使用自定义错误码、消息和原因
     */
    public GameException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.msg = message;
    }
    
    /**
     * 使用错误码枚举和原因
     */
    public GameException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
        this.msg = errorCode.getMessage();
    }
    
    // ==================== 静态工厂方法 ====================
    
    /**
     * 参数错误
     */
    public static GameException paramError(String message) {
        return new GameException(CommonErrorCode.PARAM_ERROR, message);
    }
    
    /**
     * 参数缺失
     */
    public static GameException paramMissing(String paramName) {
        return new GameException(CommonErrorCode.PARAM_MISSING, "缺少参数: " + paramName);
    }
    
    /**
     * 数据不存在
     */
    public static GameException notFound(String message) {
        return new GameException(CommonErrorCode.DATA_NOT_FOUND, message);
    }
    
    /**
     * 权限不足
     */
    public static GameException permissionDenied(String message) {
        return new GameException(CommonErrorCode.PERMISSION_DENIED, message);
    }
    
    /**
     * 操作失败
     */
    public static GameException operationFailed(String message) {
        return new GameException(CommonErrorCode.OPERATION_FAILED, message);
    }
    
    /**
     * 资源不足
     */
    public static GameException resourceNotEnough(String resourceName) {
        return new GameException(CommonErrorCode.RESOURCE_NOT_ENOUGH, resourceName + "不足");
    }
    
    /**
     * 系统错误
     */
    public static GameException systemError(String message) {
        return new GameException(CommonErrorCode.SYSTEM_ERROR, message);
    }
    
    /**
     * 系统错误（带原因）
     */
    public static GameException systemError(String message, Throwable cause) {
        return new GameException(CommonErrorCode.SYSTEM_ERROR.getCode(), message, cause);
    }
    
    // ==================== Getter ====================
    
    public int getCode() {
        return code;
    }
    
    public String getMsg() {
        return msg;
    }
    
    public Object getData() {
        return data;
    }
    
    public GameException data(Object data) {
        this.data = data;
        return this;
    }
    
    @Override
    public String toString() {
        return "GameException{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                '}';
    }
}
