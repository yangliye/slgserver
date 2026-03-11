package com.muyi.core.handler;

import com.muyi.core.module.AbstractGameModule;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * 消息处理器扫描器
 * <p>
 * 扫描指定包下所有 {@link MsgHandler} 注解标记的 {@link MessageHandler}，
 * 自动实例化并调用 {@link MessageHandler#init} 完成依赖注入。
 * <p>
 * Game / World 等模块的 Dispatcher 均可复用此扫描器。
 */
public final class HandlerScanner {

    private static final Logger log = LoggerFactory.getLogger(HandlerScanner.class);

    private HandlerScanner() {
    }

    /**
     * 扫描并初始化所有 @MsgHandler 标记的 MessageHandler
     *
     * @param packages 要扫描的包名
     * @param module   当前模块实例（传给 handler.init）
     * @return (msgId, handler) 列表
     */
    @SuppressWarnings("rawtypes")
    public static List<HandlerEntry> scan(String[] packages, AbstractGameModule module) {
        List<HandlerEntry> result = new ArrayList<>();

        try (ScanResult scanResult = new ClassGraph()
                .enableAnnotationInfo()
                .enableClassInfo()
                .acceptPackages(packages)
                .scan()) {

            for (ClassInfo classInfo : scanResult.getClassesWithAnnotation(MsgHandler.class)) {
                Class<?> clazz = classInfo.loadClass();

                if (!MessageHandler.class.isAssignableFrom(clazz)) {
                    log.warn("{} has @MsgHandler but does not implement MessageHandler, skipped",
                            clazz.getName());
                    continue;
                }

                MsgHandler annotation = clazz.getAnnotation(MsgHandler.class);
                int msgId = annotation.value();

                try {
                    Constructor<?> ctor = clazz.getDeclaredConstructor();
                    ctor.setAccessible(true);
                    MessageHandler handler = (MessageHandler) ctor.newInstance();
                    handler.init(module);
                    result.add(new HandlerEntry(msgId, handler));
                    log.debug("Scanned handler: {} -> msgId={}", clazz.getSimpleName(), msgId);
                } catch (Exception e) {
                    log.error("Failed to instantiate handler: {}", clazz.getName(), e);
                }
            }
        }

        log.info("HandlerScanner found {} handlers from {}: {}", result.size(),
                List.of(packages),
                result.stream().map(e -> e.handler().getClass().getSimpleName()).toList());

        return result;
    }

    @SuppressWarnings("rawtypes")
    public record HandlerEntry(int msgId, MessageHandler handler) {
    }
}
