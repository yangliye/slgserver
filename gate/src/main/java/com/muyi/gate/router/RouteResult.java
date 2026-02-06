package com.muyi.gate.router;

/**
 * 路由结果
 *
 * @author muyi
 */
public class RouteResult {
    
    /** 是否成功 */
    private final boolean success;
    
    /** 协议ID */
    private final int protoId;
    
    /** 路由目标 */
    private final RouteTarget target;
    
    /** 目标服务器地址 */
    private final String targetAddress;
    
    /** 消息内容 */
    private final Object message;
    
    /** 错误码 */
    private final int errorCode;
    
    /** 错误信息 */
    private final String errorMessage;
    
    private RouteResult(boolean success, int protoId, RouteTarget target, String targetAddress,
                        Object message, int errorCode, String errorMessage) {
        this.success = success;
        this.protoId = protoId;
        this.target = target;
        this.targetAddress = targetAddress;
        this.message = message;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
    
    public static RouteResult success(int protoId, RouteTarget target, String targetAddress, Object message) {
        return new RouteResult(true, protoId, target, targetAddress, message, 0, null);
    }
    
    public static RouteResult authRequired(int protoId, String errorMessage) {
        return new RouteResult(false, protoId, null, null, null, 401, errorMessage);
    }
    
    public static RouteResult migrating(int protoId, String errorMessage) {
        return new RouteResult(false, protoId, null, null, null, 503, errorMessage);
    }
    
    public static RouteResult noTarget(int protoId, String errorMessage) {
        return new RouteResult(false, protoId, null, null, null, 502, errorMessage);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public int getProtoId() {
        return protoId;
    }
    
    public RouteTarget getTarget() {
        return target;
    }
    
    public String getTargetAddress() {
        return targetAddress;
    }
    
    public Object getMessage() {
        return message;
    }
    
    public int getErrorCode() {
        return errorCode;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * 是否需要本地处理
     */
    public boolean isLocalProcess() {
        return success && target == RouteTarget.GATE_LOCAL;
    }
    
    @Override
    public String toString() {
        if (success) {
            return String.format("RouteResult[protoId=%d, target=%s, address=%s]",
                    protoId, target, targetAddress);
        }
        return String.format("RouteResult[protoId=%d, error=%d, message=%s]",
                protoId, errorCode, errorMessage);
    }
}
