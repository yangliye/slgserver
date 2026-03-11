package com.muyi.core.handler;

import java.lang.annotation.*;

/**
 * 标记消息处理器及其处理的消息 ID。
 * <p>
 * 配合 {@link HandlerScanner} 实现自动扫描注册，
 * 无需在模块中手动 register。
 *
 * <pre>{@code
 * @MsgHandler(MsgId.PLAYER_LOGIN_REQ_VALUE)
 * public class PlayerLoginHandler implements MessageHandler<PlayerLoginReq> {
 *
 *     @Override
 *     public void init(AbstractGameModule module) {
 *         // 从 module 获取依赖
 *     }
 *
 *     @Override
 *     public void handle(long uid, int msgSeq, PlayerLoginReq msg) {
 *         // 业务逻辑
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MsgHandler {

    /**
     * 消息协议 ID（使用 proto 生成的 MsgId.XXX_VALUE 常量）
     */
    int value();
}
