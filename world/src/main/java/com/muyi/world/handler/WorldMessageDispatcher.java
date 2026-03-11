package com.muyi.world.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.muyi.core.handler.HandlerScanner;
import com.muyi.core.handler.MessageHandler;
import com.muyi.core.module.AbstractGameModule;
import com.muyi.proto.MessageRegistry;
import com.muyi.world.player.WorldExecutorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * World 消息分发器
 * <p>
 * 与 Game 侧的 GameMessageDispatcher 同一模式：
 * 根据 msgId 查找 handler，解析 protobuf 后投递到条带线程执行。
 */
public class WorldMessageDispatcher {

    private static final Logger log = LoggerFactory.getLogger(WorldMessageDispatcher.class);

    private final WorldExecutorManager executorManager;

    @SuppressWarnings("rawtypes")
    private final Map<Integer, MessageHandler> handlers = new ConcurrentHashMap<>();

    public WorldMessageDispatcher(WorldExecutorManager executorManager) {
        this.executorManager = executorManager;
    }

    /**
     * 扫描并注册所有 @MsgHandler 标记的处理器
     */
    public void scanAndRegister(String[] packages, AbstractGameModule module) {
        for (HandlerScanner.HandlerEntry entry : HandlerScanner.scan(packages, module)) {
            register(entry.msgId(), entry.handler());
        }
    }

    public <T> void register(int msgId, MessageHandler<T> handler) {
        MessageHandler<?> old = handlers.putIfAbsent(msgId, handler);
        if (old != null) {
            log.warn("Duplicate world handler for msgId={}", msgId);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void dispatch(long uid, int msgId, int msgSeq, byte[] payload) {
        MessageHandler handler = handlers.get(msgId);
        if (handler == null) {
            log.warn("Player[{}] no world handler for msgId={}", uid, msgId);
            return;
        }

        Message message;
        try {
            message = MessageRegistry.parse(msgId, payload);
        } catch (InvalidProtocolBufferException e) {
            log.error("Player[{}] failed to parse world msgId={}", uid, msgId, e);
            return;
        }

        if (message == null) {
            log.warn("Player[{}] world msgId={} not registered in MessageRegistry", uid, msgId);
            return;
        }

        executorManager.dispatch(uid, () -> {
            try {
                handler.handle(uid, msgSeq, message);
            } catch (Exception e) {
                log.error("Player[{}] world handler error for msgId={}", uid, msgId, e);
            }
        });
    }

    public int handlerCount() {
        return handlers.size();
    }
}
