package com.muyi.game.data;

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
 * 持有一个在线玩家的所有 {@link AbstractPlayerManager} 实例。
 * 在玩家登录时由 {@link PlayerDataRegistry} 创建，在登出时销毁。
 *
 * @author muyi
 */
public class PlayerDataContext {

    private static final Logger log = LoggerFactory.getLogger(PlayerDataContext.class);

    private final long uid;
    private final Map<Class<?>, AbstractPlayerManager<?>> managers = new ConcurrentHashMap<>();
    private final List<AbstractPlayerManager<?>> orderedManagers;

    PlayerDataContext(long uid, DbManager db, List<PlayerDataRegistry.ManagerMeta> metas) {
        this.uid = uid;
        List<AbstractPlayerManager<?>> ordered = new ArrayList<>(metas.size());

        for (PlayerDataRegistry.ManagerMeta meta : metas) {
            try {
                AbstractPlayerManager<?> manager = meta.clazz().getDeclaredConstructor().newInstance();
                manager.init(uid, db, this);
                managers.put(meta.clazz(), manager);
                ordered.add(manager);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create manager: " + meta.clazz().getName(), e);
            }
        }
        this.orderedManagers = Collections.unmodifiableList(ordered);
    }

    /**
     * 加载所有 Manager 的数据（按 order 顺序）
     */
    public void loadAll() {
        for (AbstractPlayerManager<?> manager : orderedManagers) {
            manager.load();
        }
    }

    /**
     * 触发所有 Manager 的 onLogin 回调（在 loadAll 之后调用）
     */
    public void onLogin() {
        for (AbstractPlayerManager<?> manager : orderedManagers) {
            try {
                manager.onLogin();
            } catch (Exception e) {
                log.error("Player[{}] manager {} onLogin error", uid, manager.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * 触发所有 Manager 的 onLogout 回调并清理
     */
    public void onLogout() {
        for (int i = orderedManagers.size() - 1; i >= 0; i--) {
            try {
                orderedManagers.get(i).clear();
            } catch (Exception e) {
                log.error("Player[{}] manager logout error", uid, e);
            }
        }
        managers.clear();
    }

    /**
     * 获取指定类型的 Manager
     */
    @SuppressWarnings("unchecked")
    public <M extends AbstractPlayerManager<?>> M getManager(Class<M> clazz) {
        return (M) managers.get(clazz);
    }

    public long getUid() {
        return uid;
    }
}
