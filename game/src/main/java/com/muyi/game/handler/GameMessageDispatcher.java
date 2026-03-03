package com.muyi.game.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.muyi.game.player.PlayerExecutorManager;
import com.muyi.proto.MessageRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Game 消息分发器
 * <p>
 * 负责将 Gate 转发来的原始消息（msgId + payload）解析为 protobuf 对象，
 * 并分发到已注册的 {@link MessageHandler} 处理。
 * <p>
 * 所有消息的处理都通过 {@link PlayerExecutorManager#dispatch} 投递到玩家对应的条带线程，
 * 保证同一玩家的消息串行处理。
 *
 * <pre>{@code
 * // 初始化
 * GameMessageDispatcher dispatcher = new GameMessageDispatcher(playerExecutorManager);
 *
 * // 注册 handler
 * dispatcher.register(2001, (uid, msgSeq, msg) -> {
 *     PlayerInfoReq req = (PlayerInfoReq) msg;
 *     // 处理逻辑...
 * });
 *
 * // Gate RPC 调用后触发
 * dispatcher.dispatch(uid, msgId, msgSeq, payload);
 * }</pre>
 *
 * @author muyi
 */
public class GameMessageDispatcher {

    private static final Logger log = LoggerFactory.getLogger(GameMessageDispatcher.class);

    private final PlayerExecutorManager playerManager;

    @SuppressWarnings("rawtypes")
    private final Map<Integer, MessageHandler> handlers = new ConcurrentHashMap<>();

    public GameMessageDispatcher(PlayerExecutorManager playerManager) {
        this.playerManager = playerManager;
    }

    /**
     * 注册消息处理器
     *
     * @param msgId   消息协议 ID
     * @param handler 处理逻辑
     */
    public <T extends Message> void register(int msgId, MessageHandler<T> handler) {
        MessageHandler<?> old = handlers.putIfAbsent(msgId, handler);
        if (old != null) {
            log.warn("Duplicate handler for msgId={}", msgId);
        }
    }

    /**
     * 分发消息
     * <p>
     * 解析 protobuf 并投递到玩家对应的条带线程执行。
     *
     * @param uid     玩家 ID
     * @param msgId   消息协议 ID
     * @param msgSeq  消息序号
     * @param payload protobuf 序列化字节
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void dispatch(long uid, int msgId, int msgSeq, byte[] payload) {
        MessageHandler handler = handlers.get(msgId);
        if (handler == null) {
            log.warn("Player[{}] no handler for msgId={}", uid, msgId);
            return;
        }

        Message message;
        try {
            message = MessageRegistry.parse(msgId, payload);
        } catch (InvalidProtocolBufferException e) {
            log.error("Player[{}] failed to parse msgId={}", uid, msgId, e);
            return;
        }

        if (message == null) {
            log.warn("Player[{}] msgId={} not registered in MessageRegistry", uid, msgId);
            return;
        }

        playerManager.dispatch(uid, () -> {
            try {
                handler.handle(uid, msgSeq, message);
            } catch (Exception e) {
                log.error("Player[{}] handler error for msgId={}", uid, msgId, e);
            }
        });
    }

    /**
     * 已注册的 handler 数量
     */
    public int handlerCount() {
        return handlers.size();
    }
}
