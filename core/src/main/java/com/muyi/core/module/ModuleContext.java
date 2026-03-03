package com.muyi.core.module;

/**
 * 模块上下文
 * <p>
 * 通过 {@link #current()} 获取当前线程所属的模块实例，
 * 由 {@link com.muyi.core.thread.ThreadPoolManager} 在每次任务执行前自动设置。
 * <p>
 * 使用示例：
 * <pre>{@code
 * GameModule game = ModuleContext.current(GameModule.class);
 * int serverId = game.getServerId();
 * }</pre>
 */
public final class ModuleContext {

    private static final ThreadLocal<AbstractGameModule> CURRENT = new ThreadLocal<>();

    private ModuleContext() {
    }

    /**
     * 设置当前线程所属模块（由 ThreadPoolManager 在任务执行前调用）
     */
    public static void setCurrent(AbstractGameModule module) {
        CURRENT.set(module);
    }

    /**
     * 获取当前线程所属模块
     *
     * @throws IllegalStateException 如果当前线程不属于任何模块
     */
    public static AbstractGameModule current() {
        AbstractGameModule module = CURRENT.get();
        if (module == null) {
            throw new IllegalStateException("No module bound to current thread: " + Thread.currentThread().getName());
        }
        return module;
    }

    /**
     * 获取当前线程所属模块（带类型转换）
     * <pre>{@code
     * GameModule game = ModuleContext.current(GameModule.class);
     * int serverId = game.getServerId();
     * }</pre>
     */
    @SuppressWarnings("unchecked")
    public static <T extends AbstractGameModule> T current(Class<T> type) {
        AbstractGameModule module = current();
        if (!type.isInstance(module)) {
            throw new IllegalStateException(
                    "Current module is " + module.getClass().getSimpleName()
                            + ", not " + type.getSimpleName());
        }
        return (T) module;
    }

    /**
     * 获取当前线程所属模块，不属于任何模块返回 null
     */
    public static AbstractGameModule currentOrNull() {
        return CURRENT.get();
    }
}
