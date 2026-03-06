package com.muyi.game.playerdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 玩家组件公共基类
 * <p>
 * 所有绑定到玩家的组件（无论是否有 DB 实体）都继承此类，
 * 参与统一的生命周期管理：init → load → onLogin → onLogout → clear。
 * <p>
 * 子类体系：
 * <ul>
 *   <li>{@link AbstractPlayerManager} — 有 DB 实体的数据管理器</li>
 *   <li>{@link AbstractPlayerLogic} — 纯逻辑组件，无 DB 依赖</li>
 * </ul>
 *
 * @author muyi
 */
public abstract class AbstractPlayerComponent {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private long uid;
    private PlayerDataContext context;

    // ==================== 子类可选重写 ====================

    /**
     * 玩家登录后回调（在所有组件都加载完成后触发）
     */
    protected void onLogin() {
    }

    /**
     * 玩家登出时回调（用于清理定时器、保存临时数据等）
     */
    protected void onLogout() {
    }

    // ==================== 框架调用（包级可见）====================

    void initComponent(long uid, PlayerDataContext context) {
        this.uid = uid;
        this.context = context;
    }

    /**
     * 加载数据，默认空实现。{@link AbstractPlayerManager} 会重写此方法从 DB 加载。
     */
    void load() {
    }

    void clear() {
        onLogout();
    }

    // ==================== 工具方法 ====================

    protected long getUid() {
        return uid;
    }

    /**
     * 获取其他组件（跨组件访问）
     */
    protected <C extends AbstractPlayerComponent> C getComponent(Class<C> clazz) {
        return context.getComponent(clazz);
    }

    protected PlayerDataContext getContext() {
        return context;
    }
}
