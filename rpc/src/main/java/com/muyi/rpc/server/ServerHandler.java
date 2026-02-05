package com.muyi.rpc.server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.muyi.rpc.core.RpcFuture;
import com.muyi.rpc.core.RpcRequest;
import com.muyi.rpc.core.RpcResponse;
import com.muyi.rpc.protocol.MessageType;
import com.muyi.rpc.protocol.RpcMessage;
import com.muyi.rpc.serialize.SerializerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * 服务端业务处理器
 * 使用虚拟线程处理RPC请求（JDK 21+）
 *
 * @author muyi
 */
public class ServerHandler extends SimpleChannelInboundHandler<RpcMessage> {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);
    
    /** 业务线程池 - 使用虚拟线程（全局共享，避免每个连接创建一个） */
    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    
    /** 确保只关闭一次 */
    private static final java.util.concurrent.atomic.AtomicBoolean executorShutdown = new java.util.concurrent.atomic.AtomicBoolean(false);
    
    /**
     * 关闭共享线程池（应在应用关闭时调用，多次调用安全）
     */
    public static void shutdownExecutor() {
        // CAS 确保只执行一次
        if (!executorShutdown.compareAndSet(false, true)) {
            return;
        }
        
        EXECUTOR.shutdown();
        try {
            if (!EXECUTOR.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /** Method 缓存：serviceKey#methodName#paramTypes -> Method（避免每次反射查找） */
    private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();
    
    /** 负缓存标记对象，用于缓存不存在的方法（避免频繁反射查找） */
    private static final Method NOT_FOUND_METHOD;
    static {
        try {
            // 使用 Object.getClass() 作为占位符，表示方法不存在
            NOT_FOUND_METHOD = Object.class.getMethod("getClass");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Failed to initialize NOT_FOUND_METHOD", e);
        }
    }
    
    private final RpcServer rpcServer;
    
    public ServerHandler(RpcServer rpcServer) {
        this.rpcServer = rpcServer;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) {
        if (msg.getMessageType() != MessageType.REQUEST) {
            return;
        }
        
        if (msg.getData() instanceof RpcRequest request) {
            // 提交到共享虚拟线程池处理
            EXECUTOR.execute(() -> {
                RpcResponse response = handleRequest(request);
                
                // 如果是单向调用，不需要返回响应
                if (request.isOneWay()) {
                    return;
                }
                
                // 发送响应
                RpcMessage responseMsg = new RpcMessage(
                        MessageType.RESPONSE,
                        SerializerFactory.getDefaultType(),
                        request.getRequestId(),
                        response
                );
                
                if (ctx.channel().isActive()) {
                    ctx.writeAndFlush(responseMsg);
                }
            });
        }
    }
    
    /**
     * 处理RPC请求
     */
    private RpcResponse handleRequest(RpcRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 获取服务实例
            String serviceKey = request.getServiceKey();
            Object service = rpcServer.getService(serviceKey);
            
            if (service == null) {
                logger.warn("Service not found: {}", serviceKey);
                return RpcResponse.fail(request.getRequestId(), "Service not found: " + serviceKey);
            }
            
            String methodName = request.getMethodName();
            Class<?>[] paramTypes = request.getParameterTypes();
            
            // 尝试获取方法
            Method method = findMethod(service.getClass(), methodName, paramTypes);
            
            if (method == null) {
                logger.warn("Method not found: {}.{}", serviceKey, methodName);
                return RpcResponse.fail(request.getRequestId(), 
                        "Method not found: " + serviceKey + "." + methodName);
            }
            
            // 检查是否是异步方法（返回类型是 Future/RpcFuture）
            // 如果是，尝试调用对应的同步方法
            if (isAsyncReturnType(method.getReturnType())) {
                Method syncMethod = findSyncMethod(service.getClass(), methodName, paramTypes);
                if (syncMethod != null) {
                    method = syncMethod;
                }
            }
            
            // 调用方法（确保 parameters 不为 null，避免 invoke 问题）
            Object[] params = request.getParameters();
            Object result = method.invoke(service, params != null ? params : new Object[0]);
            
            if (logger.isDebugEnabled()) {
                logger.debug("Handle request: {} cost {}ms", 
                        request, System.currentTimeMillis() - startTime);
            }
            
            return RpcResponse.success(request.getRequestId(), result);
            
        } catch (InvocationTargetException e) {
            // 解包反射调用异常，获取真实的业务异常
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            logger.error("Handle request error (business exception): {}", request, cause);
            return RpcResponse.error(request.getRequestId(), cause);
        } catch (Exception e) {
            logger.error("Handle request error: {}", request, e);
            return RpcResponse.error(request.getRequestId(), e);
        }
    }
    
    /**
     * 查找方法（带缓存和负缓存，避免每次反射查找）
     */
    private Method findMethod(Class<?> clazz, String methodName, Class<?>[] paramTypes) {
        // 构建缓存 key
        String cacheKey = clazz.getName() + "#" + methodName + "#" + Arrays.toString(paramTypes);
        
        // 先查缓存（使用 get 而非 containsKey + get，保证原子性）
        Method cached = METHOD_CACHE.get(cacheKey);
        if (cached != null) {
            // 检查是否是负缓存标记
            if (cached == NOT_FOUND_METHOD) {
                return null;
            }
            return cached;
        }
        
        // 未命中缓存，进行反射查找
        try {
            Method method = clazz.getMethod(methodName, paramTypes);
            METHOD_CACHE.put(cacheKey, method);
            return method;
        } catch (NoSuchMethodException _) {
            // 方法不存在时缓存负标记，避免频繁反射查找恶意请求
            METHOD_CACHE.put(cacheKey, NOT_FOUND_METHOD);
            return null;
        }
    }
    
    /**
     * 检查返回类型是否是异步类型
     */
    private boolean isAsyncReturnType(Class<?> returnType) {
        return Future.class.isAssignableFrom(returnType) || 
               RpcFuture.class.isAssignableFrom(returnType);
    }
    
    /**
     * 查找异步方法对应的同步方法
     * 规则：xxxAsync -> xxx
     */
    private Method findSyncMethod(Class<?> clazz, String asyncMethodName, Class<?>[] paramTypes) {
        if (!asyncMethodName.endsWith("Async")) {
            return null;
        }
        
        String syncMethodName = asyncMethodName.substring(0, asyncMethodName.length() - 5);
        return findMethod(clazz, syncMethodName, paramTypes);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Server handler error, channel: {}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // 使用 debug 级别，因为客户端断开连接可能很频繁
        if (logger.isDebugEnabled()) {
            logger.debug("Client disconnected: {}", ctx.channel().remoteAddress());
        }
    }
}
