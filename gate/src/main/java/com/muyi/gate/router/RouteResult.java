package com.muyi.gate.router;

/**
 * 路由结果
 *
 * @author muyi
 */
public class RouteResult {
    
    private final boolean success;
    private final int protoId;
    private final RouteTarget target;
    private final Object message;
    private final int errorCode;
    private final String errorMessage;
    
    private RouteResult(boolean success, int protoId, RouteTarget target,
                        Object message, int errorCode, String errorMessage) {
        this.success = success;
        this.protoId = protoId;
        this.target = target;
        this.message = message;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
    
    public static RouteResult success(int protoId, RouteTarget target, Object message) {
        return new RouteResult(true, protoId, target, message, 0, null);
    }
    
    public static RouteResult authRequired(int protoId, String errorMessage) {
        return new RouteResult(false, protoId, null, null, 401, errorMessage);
    }
    
    public static RouteResult migrating(int protoId, String errorMessage) {
        return new RouteResult(false, protoId, null, null, 503, errorMessage);
    }
    
    public static RouteResult noTarget(int protoId, String errorMessage) {
        return new RouteResult(false, protoId, null, null, 502, errorMessage);
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
    
    public Object getMessage() {
        return message;
    }
    
    public int getErrorCode() {
        return errorCode;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public boolean isLocalProcess() {
        return success && target == RouteTarget.GATE_LOCAL;
    }
    
    @Override
    public String toString() {
        if (success) {
            return String.format("RouteResult[protoId=%d, target=%s]", protoId, target);
        }
        return String.format("RouteResult[protoId=%d, error=%d, message=%s]",
                protoId, errorCode, errorMessage);
    }
}
