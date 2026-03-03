package com.muyi.proto;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 消息注册表
 * <p>
 * 管理 msgId 与 protobuf Parser 的映射关系。
 * 支持从 .proto FileDescriptor 自动扫描注册带有 {@code option (msgId)} 的消息。
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 自动注册（推荐）：传入 .proto 生成的外部类的 FileDescriptor
 * MessageRegistry.registerFromDescriptors(Gate.getDescriptor(), Game.getDescriptor());
 *
 * // 手动注册
 * MessageRegistry.register(9001, GateAuthReq.parser());
 *
 * // 解析
 * Message msg = MessageRegistry.parse(9001, payload);
 *
 * // 序列化
 * byte[] bytes = MessageRegistry.toBytes(gateAuthResp);
 * }</pre>
 */
public final class MessageRegistry {

    private static final Logger logger = LoggerFactory.getLogger(MessageRegistry.class);

    private static final Map<Integer, Parser<? extends Message>> PARSERS = new ConcurrentHashMap<>();
    private static volatile boolean initialized;

    private MessageRegistry() {
    }

    /**
     * 初始化：自动扫描所有已知 proto 文件并注册消息。幂等，可安全多次调用。
     */
    public static void init() {
        if (initialized) {
            return;
        }
        synchronized (MessageRegistry.class) {
            if (initialized) {
                return;
            }
            registerFromDescriptors(
                    com.muyi.proto.gate.Gate.getDescriptor(),
                    com.muyi.proto.game.Game.getDescriptor()
            );
            initialized = true;
        }
    }

    /**
     * 从 FileDescriptor 自动扫描并注册所有带 msgId 选项的消息
     * <p>
     * 遍历 .proto 文件中定义的所有 message，提取 {@code option (msgId)} 的值，
     * 通过反射找到对应的 Java 类并获取其 Parser，完成自动注册。
     *
     * @param fileDescriptors .proto 文件对应的 FileDescriptor（通过生成的外部类 .getDescriptor() 获取）
     */
    public static void registerFromDescriptors(Descriptors.FileDescriptor... fileDescriptors) {
        for (Descriptors.FileDescriptor fd : fileDescriptors) {
            String javaPackage = fd.getOptions().getJavaPackage();
            for (Descriptors.Descriptor md : fd.getMessageTypes()) {
                if (!md.getOptions().hasExtension(MsgOptions.msgId)) {
                    continue;
                }
                int id = md.getOptions().getExtension(MsgOptions.msgId);
                String className = javaPackage + "." + md.getName();
                try {
                    Class<?> clazz = Class.forName(className);
                    Method parserMethod = clazz.getMethod("parser");
                    @SuppressWarnings("unchecked")
                    Parser<? extends Message> parser = (Parser<? extends Message>) parserMethod.invoke(null);
                    register(id, parser);
                    logger.debug("Auto-registered msgId={} -> {}", id, className);
                } catch (Exception e) {
                    logger.error("Failed to auto-register msgId={} for {}", id, className, e);
                }
            }
        }
        logger.info("MessageRegistry: {} messages registered from {} proto files",
                PARSERS.size(), fileDescriptors.length);
    }

    /**
     * 手动注册消息解析器
     *
     * @param msgId  消息 ID
     * @param parser protobuf Parser
     */
    public static void register(int msgId, Parser<? extends Message> parser) {
        Parser<? extends Message> old = PARSERS.putIfAbsent(msgId, parser);
        if (old != null) {
            logger.warn("Duplicate msgId registration: {}", msgId);
        }
    }

    /**
     * 解析 protobuf 消息
     *
     * @param msgId   消息 ID
     * @param payload 序列化字节
     * @return 解析后的 Message，未注册返回 null
     */
    public static Message parse(int msgId, byte[] payload) throws InvalidProtocolBufferException {
        Parser<? extends Message> parser = PARSERS.get(msgId);
        if (parser == null) {
            logger.warn("No parser registered for msgId: {}", msgId);
            return null;
        }
        return parser.parseFrom(payload);
    }

    /**
     * 序列化 protobuf 消息
     */
    public static byte[] toBytes(Message msg) {
        return msg.toByteArray();
    }

    /**
     * 通过 msgId 获取消息的 Parser
     */
    public static Parser<? extends Message> getParser(int msgId) {
        return PARSERS.get(msgId);
    }

    /**
     * 检查 msgId 是否已注册
     */
    public static boolean isRegistered(int msgId) {
        return PARSERS.containsKey(msgId);
    }

    /**
     * 获取已注册的消息数量
     */
    public static int registeredCount() {
        return PARSERS.size();
    }
}
