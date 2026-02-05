package com.muyi.rpc.core;

import com.muyi.common.util.time.TimeUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RPC请求对象
 * 封装远程调用的所有必要信息
 *
 * @author muyi
 */
public class RpcRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    private static final AtomicLong REQUEST_ID_GEN = new AtomicLong(0);
    
    /** 请求ID */
    private long requestId;
    
    /** 服务接口名 */
    private String interfaceName;
    
    /** 方法名 */
    private String methodName;
    
    /** 参数类型 */
    private Class<?>[] parameterTypes;
    
    /** 参数值 */
    private Object[] parameters;
    
    /** 服务器ID（如 1, 2, 3），0 表示不指定 */
    private int serverId;
    
    /** 请求时间戳 */
    private long timestamp;
    
    /** 是否单向调用（不需要返回值） */
    private boolean oneWay;
    
    public RpcRequest() {
        this.requestId = REQUEST_ID_GEN.incrementAndGet();
        this.timestamp = TimeUtils.currentTimeMillis();
    }
    
    /**
     * 生成服务唯一标识
     */
    public String getServiceKey() {
        return ServiceKey.build(interfaceName, serverId);
    }
    
    // Getters and Setters
    
    public long getRequestId() {
        return requestId;
    }
    
    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }
    
    public String getInterfaceName() {
        return interfaceName;
    }
    
    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }
    
    public String getMethodName() {
        return methodName;
    }
    
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
    
    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }
    
    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }
    
    public Object[] getParameters() {
        return parameters;
    }
    
    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }
    
    public int getServerId() {
        return serverId;
    }
    
    public void setServerId(int serverId) {
        this.serverId = serverId;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isOneWay() {
        return oneWay;
    }
    
    public void setOneWay(boolean oneWay) {
        this.oneWay = oneWay;
    }
    
    @Override
    public String toString() {
        return "RpcRequest{" +
                "requestId=" + requestId +
                ", interfaceName='" + interfaceName + '\'' +
                ", methodName='" + methodName + '\'' +
                ", parameterTypes=" + Arrays.toString(parameterTypes) +
                ", serverId=" + serverId +
                '}';
    }
}

