package com.muyi.game.playerdata;

import com.muyi.db.DbManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家数据上下文
 * <p>
 * 持有一个在线玩家的所有 {@link AbstractPlayerComponent} 实例。
 * 在玩家登录时由 {@link PlayerDataRegistry} 创建，在登出时销毁。
 *
 * @author muyi
 */
public class PlayerDataContext {

    private static final Logger log = LoggerFactory.getLogger(PlayerDataContext.class);

    private final long uid;
    private final DbManager db;
    private final Map<Class<?>, AbstractPlayerComponent> components = new ConcurrentHashMap<>();
    private final List<AbstractPlayerComponent> orderedComponents;

    PlayerDataContext(long uid, DbManager db, List<PlayerDataRegistry.ComponentMeta> metas) {
        this.uid = uid;
        this.db = db;
        List<AbstractPlayerComponent> ordered = new ArrayList<>(metas.size());

        for (PlayerDataRegistry.ComponentMeta meta : metas) {
            try {
                AbstractPlayerComponent component = meta.clazz().getDeclaredConstructor().newInstance();
                component.initComponent(uid, this);
                components.put(meta.clazz(), component);
                ordered.add(component);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create component: " + meta.clazz().getName(), e);
            }
        }
        this.orderedComponents = Collections.unmodifiableList(ordered);
    }

    /**
     * 加载所有组件的数据（按 order 顺序，仅 Manager 类型会实际查 DB）
     */
    public void loadAll() {
        for (AbstractPlayerComponent component : orderedComponents) {
            component.load();
        }
    }

    /**
     * 触发所有组件的 onLogin 回调（在 loadAll 之后调用）
     */
    public void onLogin() {
        for (AbstractPlayerComponent component : orderedComponents) {
            try {
                component.onLogin();
            } catch (Exception e) {
                log.error("Player[{}] component {} onLogin error",
                        uid, component.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * 触发所有组件的 onLogout 回调并清理（逆序）
     */
    public void onLogout() {
        for (int i = orderedComponents.size() - 1; i >= 0; i--) {
            try {
                orderedComponents.get(i).clear();
            } catch (Exception e) {
                log.error("Player[{}] component logout error", uid, e);
            }
        }
        components.clear();
    }

    /**
     * 获取指定类型的组件（Manager 或 Logic）
     */
    @SuppressWarnings("unchecked")
    public <C extends AbstractPlayerComponent> C getComponent(Class<C> clazz) {
        return (C) components.get(clazz);
    }

    /**
     * 获取指定类型的 Manager（便捷方法，等价于 getComponent）
     */
    @SuppressWarnings("unchecked")
    public <M extends AbstractPlayerManager<?, ?>> M getManager(Class<M> clazz) {
        return (M) components.get(clazz);
    }

    public long getUid() {
        return uid;
    }

    DbManager db() {
        return db;
    }
}
