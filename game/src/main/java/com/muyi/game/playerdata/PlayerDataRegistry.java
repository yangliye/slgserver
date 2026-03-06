package com.muyi.game.playerdata;

import com.muyi.db.DbManager;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 玩家数据注册中心
 * <p>
 * 扫描 {@link PlayerData} 注解，收集所有 {@link AbstractPlayerComponent} 子类，
 * 并在玩家登录时按顺序创建 {@link PlayerDataContext}。
 * <p>
 * 支持两种组件类型：
 * <ul>
 *   <li>{@link AbstractPlayerManager} — 有 DB 实体，自动加载数据</li>
 *   <li>{@link AbstractPlayerLogic} — 纯逻辑，无 DB 依赖</li>
 * </ul>
 *
 * @author muyi
 */
public class PlayerDataRegistry {

    private static final Logger log = LoggerFactory.getLogger(PlayerDataRegistry.class);

    private final List<ComponentMeta> registeredComponents = new ArrayList<>();
    private final DbManager db;

    public PlayerDataRegistry(DbManager db) {
        this.db = db;
    }

    /**
     * 扫描指定包下所有 @PlayerData 注解的组件
     */
    @SuppressWarnings("unchecked")
    public void scan(String... packageNames) {
        try (ScanResult scanResult = new ClassGraph()
                .enableAnnotationInfo()
                .enableClassInfo()
                .acceptPackages(packageNames)
                .scan()) {

            for (ClassInfo classInfo : scanResult.getClassesWithAnnotation(PlayerData.class)) {
                Class<?> clazz = classInfo.loadClass();

                if (!AbstractPlayerComponent.class.isAssignableFrom(clazz)) {
                    log.warn("{} has @PlayerData but does not extend AbstractPlayerComponent, skipped",
                            clazz.getName());
                    continue;
                }

                PlayerData annotation = clazz.getAnnotation(PlayerData.class);
                int order = annotation.order();

                registeredComponents.add(new ComponentMeta(
                        (Class<? extends AbstractPlayerComponent>) clazz, order));
                log.debug("Registered PlayerComponent: {} (order={})", clazz.getSimpleName(), order);
            }
        }

        registeredComponents.sort(Comparator.comparingInt(ComponentMeta::order));
        log.info("PlayerDataRegistry scanned, found {} components: {}", registeredComponents.size(),
                registeredComponents.stream().map(m -> m.clazz().getSimpleName()).toList());
    }

    /**
     * 手动注册组件（用于不走扫描的场景）
     */
    public void register(Class<? extends AbstractPlayerComponent> clazz, int order) {
        registeredComponents.add(new ComponentMeta(clazz, order));
        registeredComponents.sort(Comparator.comparingInt(ComponentMeta::order));
    }

    /**
     * 为玩家创建数据上下文（登录时调用）
     */
    public PlayerDataContext createContext(long uid) {
        return new PlayerDataContext(uid, db, registeredComponents);
    }

    public int getManagerCount() {
        return registeredComponents.size();
    }

    record ComponentMeta(Class<? extends AbstractPlayerComponent> clazz, int order) {
    }
}
