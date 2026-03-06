package com.muyi.game.data;

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
 * 扫描 {@link PlayerData} 注解，收集所有 Manager 类型，
 * 并在玩家登录时按顺序创建 {@link PlayerDataContext}。
 *
 * @author muyi
 */
public class PlayerDataRegistry {

    private static final Logger log = LoggerFactory.getLogger(PlayerDataRegistry.class);

    private final List<ManagerMeta> registeredManagers = new ArrayList<>();
    private final DbManager db;

    public PlayerDataRegistry(DbManager db) {
        this.db = db;
    }

    /**
     * 扫描指定包下所有 @PlayerData 注解的 Manager
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

                if (!AbstractPlayerManager.class.isAssignableFrom(clazz)) {
                    log.warn("{} has @PlayerData but does not extend AbstractPlayerManager, skipped",
                            clazz.getName());
                    continue;
                }

                PlayerData annotation = clazz.getAnnotation(PlayerData.class);
                int order = annotation.order();

                registeredManagers.add(new ManagerMeta(
                        (Class<? extends AbstractPlayerManager<?>>) clazz, order));
                log.debug("Registered PlayerManager: {} (order={})", clazz.getSimpleName(), order);
            }
        }

        registeredManagers.sort(Comparator.comparingInt(ManagerMeta::order));
        log.info("PlayerDataRegistry scanned, found {} managers: {}", registeredManagers.size(),
                registeredManagers.stream().map(m -> m.clazz().getSimpleName()).toList());
    }

    /**
     * 手动注册 Manager（用于不走扫描的场景）
     */
    public void register(Class<? extends AbstractPlayerManager<?>> clazz, int order) {
        registeredManagers.add(new ManagerMeta(clazz, order));
        registeredManagers.sort(Comparator.comparingInt(ManagerMeta::order));
    }

    /**
     * 为玩家创建数据上下文（登录时调用）
     */
    public PlayerDataContext createContext(long uid) {
        return new PlayerDataContext(uid, db, registeredManagers);
    }

    public int getManagerCount() {
        return registeredManagers.size();
    }

    /**
     * Manager 元信息
     */
    record ManagerMeta(Class<? extends AbstractPlayerManager<?>> clazz, int order) {
    }
}
