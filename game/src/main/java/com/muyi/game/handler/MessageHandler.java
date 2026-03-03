package com.muyi.game.handler;

import com.google.protobuf.Message;

/**
 * 消息处理器
 * <p>
 * 业务模块实现此接口处理特定类型的 protobuf 消息。
 *
 * <pre>{@code
 * dispatcher.register(2001, (uid, msgSeq, msg) -> {
 *     PlayerInfoReq req = (PlayerInfoReq) msg;
 *     // 处理逻辑...
 * });
 * }</pre>
 *
 * @param <T> protobuf 消息类型
 */
@FunctionalInterface
public interface MessageHandler<T extends Message> {

    /**
     * @param uid    玩家 ID
     * @param msgSeq 消息序号（用于请求-响应配对）
     * @param msg    解析后的 protobuf 消息
     */
    void handle(long uid, int msgSeq, T msg);
}
