package com.muyi.rpc.core;

import com.muyi.common.util.time.TimeUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.LoggerFactory;

import io.netty.util.Timeout;

/**
 * RPC异步调用Future
 * 支持同步等待和异步回调
 *
 * @author muyi
 */
public class RpcFuture implements Future<Object> {
    
    private final long requestId;
    private final long startTime;
    private final long timeout;
    
    private volatile RpcResponse response;
    private final CountDownLatch latch = new CountDownLatch(1);
    
    /** 确保 complete 只执行一次 */
    private final java.util.concurrent.atomic.AtomicBoolean completed = new java.util.concurrent.atomic.AtomicBoolean(false);
    
    /** 使用锁保护回调的设置和执行，避免竞态条件 */
    private final Object callbackLock = new Object();
    private Consumer<Object> successCallback;
    private Consumer<Throwable> failCallback;
    private BiConsumer<Object, Throwable> completeCallback;
    private volatile boolean callbackInvoked = false;
    
    /** 时间轮超时任务（用于在响应到达时取消超时检测） */
    private volatile Timeout timeoutTask;
    
    public RpcFuture(long requestId, long timeout) {
        this.requestId = requestId;
        this.startTime = TimeUtils.currentTimeMillis();
        this.timeout = timeout;
    }
    
    /**
     * 设置响应结果（线程安全，只执行一次）
     */
    public void complete(RpcResponse response) {
        // CAS 确保只执行一次（超时任务和正常响应可能同时触发）
        if (!completed.compareAndSet(false, true)) {
            return;
        }
        
        this.response = response;
        latch.countDown();
        
        // 执行回调
        invokeCallbacks();
    }
    
    /**
     * 执行回调（线程安全）
     */
    private void invokeCallbacks() {
        synchronized (callbackLock) {
            if (callbackInvoked || response == null) {
                return;
            }
            callbackInvoked = true;
            
            // 执行完成回调
            if (completeCallback != null) {
                try {
                    if (response.isSuccess()) {
                        completeCallback.accept(response.getData(), null);
                    } else {
                        completeCallback.accept(null, response.getException());
                    }
                } catch (Exception e) {
                    // 记录回调异常，便于排查问题
                    LoggerFactory.getLogger(RpcFuture.class)
                            .warn("Complete callback execution failed for request {}", requestId, e);
                }
            }
            
            // 执行成功/失败回调
            if (response.isSuccess() && successCallback != null) {
                try {
                    successCallback.accept(response.getData());
                } catch (Exception e) {
                    LoggerFactory.getLogger(RpcFuture.class)
                            .warn("Success callback execution failed for request {}", requestId, e);
                }
            } else if (!response.isSuccess() && failCallback != null) {
                try {
                    failCallback.accept(response.getException());
                } catch (Exception e) {
                    LoggerFactory.getLogger(RpcFuture.class)
                            .warn("Fail callback execution failed for request {}", requestId, e);
                }
            }
        }
    }
    
    /**
     * 设置成功回调（直接返回结果数据，无需手动转换）
     * 
     * @param callback 回调函数，参数为RPC调用的返回值
     */
    public RpcFuture onSuccess(Consumer<Object> callback) {
        synchronized (callbackLock) {
            this.successCallback = callback;
            // 如果响应已经到达，立即执行回调
            if (response != null && !callbackInvoked) {
                invokeCallbacks();
            }
        }
        return this;
    }
    
    /**
     * 设置失败回调
     * 
     * @param callback 回调函数，参数为异常信息
     */
    public RpcFuture onFail(Consumer<Throwable> callback) {
        synchronized (callbackLock) {
            this.failCallback = callback;
            if (response != null && !callbackInvoked) {
                invokeCallbacks();
            }
        }
        return this;
    }
    
    /**
     * 设置完成回调（无论成功失败都会调用）
     * 
     * @param callback 回调函数，参数为 (result, error)，成功时error为null，失败时result为null
     */
    public RpcFuture whenComplete(BiConsumer<Object, Throwable> callback) {
        synchronized (callbackLock) {
            this.completeCallback = callback;
            if (response != null && !callbackInvoked) {
                invokeCallbacks();
            }
        }
        return this;
    }
    
    /** 是否被取消 */
    private volatile boolean cancelled = false;
    
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        // 如果已经完成，无法取消
        if (completed.get()) {
            return false;
        }
        // 尝试标记为取消
        if (completed.compareAndSet(false, true)) {
            cancelled = true;
            // 取消超时任务
            cancelTimeoutTask();
            // 释放等待的线程
            latch.countDown();
            return true;
        }
        return false;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public boolean isDone() {
        return completed.get();
    }
    
    @Override
    public Object get() throws InterruptedException, ExecutionException {
        try {
            return get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new ExecutionException("RPC call timeout", e);
        }
    }
    
    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean success = latch.await(timeout, unit);
        if (!success) {
            throw new TimeoutException("RPC call timeout after " + timeout + " " + unit);
        }
        
        // 检查是否被取消
        if (cancelled) {
            throw new java.util.concurrent.CancellationException("RPC call was cancelled");
        }
        
        if (response == null) {
            throw new ExecutionException("Response is null", null);
        }
        
        if (!response.isSuccess()) {
            if (response.getException() != null) {
                throw new ExecutionException(response.getException());
            }
            throw new ExecutionException(response.getMessage(), null);
        }
        
        return response.getData();
    }
    
    /**
     * 获取请求ID
     */
    public long getRequestId() {
        return requestId;
    }
    
    /**
     * 检查是否超时
     */
    public boolean isTimeout() {
        return TimeUtils.currentTimeMillis() - startTime > timeout;
    }
    
    /**
     * 获取响应
     */
    public RpcResponse getResponse() {
        return response;
    }
    
    /**
     * 设置时间轮超时任务
     */
    public void setTimeoutTask(Timeout timeoutTask) {
        this.timeoutTask = timeoutTask;
    }
    
    /**
     * 取消时间轮超时任务（响应到达时调用，避免重复触发超时）
     */
    public void cancelTimeoutTask() {
        if (timeoutTask != null && !timeoutTask.isCancelled()) {
            timeoutTask.cancel();
        }
    }
}
