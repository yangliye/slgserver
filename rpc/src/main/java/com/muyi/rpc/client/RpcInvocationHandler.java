package com.muyi.rpc.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.muyi.rpc.annotation.RpcTimeout;
import com.muyi.rpc.core.RpcFuture;
import com.muyi.rpc.core.RpcRequest;

/**
 * RPC调用处理器
 * 
 * 调用模式自动判断：
 * - 返回值为 void：单向调用（不等待响应）
 * - 返回值为 RpcFuture/CompletableFuture/Future：异步调用
 * - 其他返回值：同步调用
 *
 * @author muyi
 */
public class RpcInvocationHandler implements InvocationHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(RpcInvocationHandler.class);
    
    private final String interfaceName;
    private final int serverId;
    private final long timeout;
    private final int retries;
    private final RpcClient client;
    
    public RpcInvocationHandler(Class<?> interfaceClass, int serverId,
                                long timeout, int retries, RpcClient client) {
        this.interfaceName = interfaceClass.getName();
        this.serverId = serverId;
        this.timeout = timeout;
        this.retries = retries;
        this.client = client;
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 处理 Object 方法（toString, hashCode, equals）
        String methodName = method.getName();
        if (Object.class.equals(method.getDeclaringClass())) {
            if ("toString".equals(methodName)) {
                return "RpcProxy[" + interfaceName + "#" + serverId + "]";
            }
            if ("hashCode".equals(methodName)) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(methodName)) {
                return proxy == args[0];
            }
            // 其他 Object 方法（如 getClass）
            return method.invoke(this, args);
        }
        
        // 获取方法级别的超时配置（优先使用 @RpcTimeout 注解）
        long methodTimeout = this.timeout;
        int methodRetries = this.retries;
        
        RpcTimeout timeoutAnnotation = method.getAnnotation(RpcTimeout.class);
        if (timeoutAnnotation != null) {
            methodTimeout = timeoutAnnotation.value();
            if (timeoutAnnotation.retries() >= 0) {
                methodRetries = timeoutAnnotation.retries();
            }
        }
        
        // 构建请求
        RpcRequest request = new RpcRequest();
        request.setInterfaceName(interfaceName);
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParameters(args);
        request.setServerId(serverId);
        
        // 根据方法返回值类型自动选择调用方式
        Class<?> returnType = method.getReturnType();
        
        // 1. void返回值 → 单向调用
        if (returnType == void.class || returnType == Void.class) {
            client.invokeOneWay(request);
            return null;
        }
        
        // 2. Future类型返回值 → 异步调用
        if (Future.class.isAssignableFrom(returnType) || 
            CompletableFuture.class.isAssignableFrom(returnType) ||
            RpcFuture.class.isAssignableFrom(returnType)) {
            return client.invokeAsync(request, methodTimeout);
        }
        
        // 3. 其他返回值类型 → 同步调用（带重试）
        return invokeSync(request, methodTimeout, methodRetries);
    }
    
    /** 初始重试延迟（毫秒） */
    private static final long INITIAL_RETRY_DELAY_MS = 100;
    /** 最大重试延迟（毫秒） */
    private static final long MAX_RETRY_DELAY_MS = 2000;
    
    /**
     * 同步调用（带重试，使用指数退避）
     * 注意：每次重试创建新的 RpcRequest（新的 requestId），避免响应混淆
     */
    private Object invokeSync(RpcRequest request, long timeout, int retries) throws Exception {
        Exception lastException = null;
        long delay = INITIAL_RETRY_DELAY_MS;
        
        for (int i = 0; i <= retries; i++) {
            try {
                // 重试时创建新的 request（新的 requestId），避免与之前超时的请求响应混淆
                RpcRequest retryRequest = (i == 0) ? request : createRetryRequest(request);
                return client.invoke(retryRequest, timeout);
            } catch (Exception e) {
                lastException = e;
                if (i < retries) {
                    logger.warn("RPC call failed, retrying... ({}/{}) after {}ms", i + 1, retries, delay);
                    // 重试前延迟（指数退避），避免连续快速重试
                    try {
                        Thread.sleep(delay);
                        // 指数退避，但不超过最大值
                        delay = Math.min(delay * 2, MAX_RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e; // 中断时直接抛出原异常
                    }
                }
            }
        }
        if (lastException != null) {
            throw lastException;
        }
        return null;
    }
    
    /**
     * 创建重试请求（复用原请求内容但生成新的 requestId）
     */
    private RpcRequest createRetryRequest(RpcRequest original) {
        RpcRequest retry = new RpcRequest(); // 构造时自动生成新的 requestId
        retry.setInterfaceName(original.getInterfaceName());
        retry.setMethodName(original.getMethodName());
        retry.setParameterTypes(original.getParameterTypes());
        retry.setParameters(original.getParameters());
        retry.setServerId(original.getServerId());
        retry.setOneWay(original.isOneWay());
        return retry;
    }
}

