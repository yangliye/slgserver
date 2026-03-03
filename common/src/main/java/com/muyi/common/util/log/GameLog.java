package com.muyi.common.util.log;

/**
 * 游戏日志上下文工具
 * <p>
 * 基于 InheritableThreadLocal 实现模块信息的自动传递。
 * 子线程（含虚拟线程）自动继承父线程的模块上下文，业务代码无需关心。
 * <p>
 * 模块信息的最终输出由 Logback 自定义 Converter（ModuleConverter / ServerIdConverter）完成，
 * Converter 优先通过包名注册表匹配，fallback 到本类的 ITL 值。
 *
 * @author muyi
 */
public final class GameLog {

    private static final InheritableThreadLocal<String> ITL_MODULE = new InheritableThreadLocal<>();
    private static final InheritableThreadLocal<String> ITL_SERVER_ID = new InheritableThreadLocal<>();

    private GameLog() {}

    /**
     * 设置当前线程的日志上下文（模块启动时调用）
     */
    public static void init(String moduleType, int serverId) {
        ITL_MODULE.set(moduleType);
        ITL_SERVER_ID.set(String.valueOf(serverId));
    }

    /**
     * 设置当前线程的日志上下文（框架内部用，如 RPC/HTTP 线程入口）
     */
    public static void set(String moduleType, String serverId) {
        ITL_MODULE.set(moduleType);
        ITL_SERVER_ID.set(serverId);
    }

    /**
     * 清除当前线程的日志上下文
     */
    public static void clear() {
        ITL_MODULE.remove();
        ITL_SERVER_ID.remove();
    }

    /**
     * 获取当前线程的模块类型
     */
    public static String moduleType() {
        return ITL_MODULE.get();
    }

    /**
     * 获取当前线程的服务器ID
     */
    public static String serverId() {
        return ITL_SERVER_ID.get();
    }
}
