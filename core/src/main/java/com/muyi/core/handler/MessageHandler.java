package com.muyi.core.handler;

import com.muyi.core.module.AbstractGameModule;

/**
 * 消息处理器
 * <p>
 * 业务模块实现此接口处理特定类型的消息（通常是 protobuf）。
 * 配合 {@link MsgHandler} 注解实现自动扫描注册。
 * <p>
 * 使用方式：
 * <ol>
 *   <li>类上标注 {@code @MsgHandler(msgId)}</li>
 *   <li>提供无参构造函数</li>
 *   <li>重写 {@link #init} 从 module 获取依赖</li>
 *   <li>实现 {@link #handle} 处理消息</li>
 * </ol>
 *
 * @param <T> 消息类型
 */
public interface MessageHandler<T> {

    /**
     * @param uid    玩家 ID
     * @param msgSeq 消息序号（用于请求-响应配对）
     * @param msg    解析后的 protobuf 消息
     */
    void handle(long uid, int msgSeq, T msg);

    /**
     * 初始化回调，扫描实例化后由 {@link HandlerScanner} 调用。
     * <p>
     * 重写此方法从 module 获取依赖。
     *
     * @param module 当前模块实例
     */
    default void init(AbstractGameModule module) {
        // no-op by default
    }
}
