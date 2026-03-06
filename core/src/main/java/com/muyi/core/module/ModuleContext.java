package com.muyi.core.module;

/**
 * 模块上下文（基于 ScopedValue）
 * <p>
 * 通过 {@link #current()} 获取当前作用域所属的模块实例。
 * 由框架在任务执行时通过 {@link #runWith(AbstractGameModule, Runnable)} 自动绑定。
 * <p>
 * 相比 ThreadLocal 的优势：
 * <ul>
 *   <li>不可变 — 作用域内无法修改，避免意外篡改</li>
 *   <li>自动清理 — 作用域结束值自动消失，不可能泄漏</li>
 *   <li>虚拟线程友好 — 性能更优</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>{@code
 * GameModule game = ModuleContext.current(GameModule.class);
 * int serverId = game.getServerId();
 * }</pre>
 */
public final class ModuleContext {

    private static final ScopedValue<AbstractGameModule> CURRENT = ScopedValue.newInstance();

    private ModuleContext() {
    }

    /**
     * 在指定模块上下文中执行任务
     *
     * @param module 模块实例
     * @param task   执行体
     */
    public static void runWith(AbstractGameModule module, Runnable task) {
        ScopedValue.where(CURRENT, module).run(task);
    }

    /**
     * 在指定模块上下文中执行任务（有返回值）
     */
    public static <R> R callWith(AbstractGameModule module, java.util.concurrent.Callable<R> task) throws Exception {
        return ScopedValue.where(CURRENT, module).call(task::call);
    }

    /**
     * 获取当前作用域所属模块
     *
     * @throws IllegalStateException 如果当前作用域不属于任何模块
     */
    public static AbstractGameModule current() {
        if (!CURRENT.isBound()) {
            throw new IllegalStateException("No module bound to current scope: " + Thread.currentThread().getName());
        }
        return CURRENT.get();
    }

    /**
     * 获取当前作用域所属模块（带类型转换）
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
     * 获取当前作用域所属模块，不属于任何模块返回 null
     */
    public static AbstractGameModule currentOrNull() {
        return CURRENT.isBound() ? CURRENT.get() : null;
    }
}
