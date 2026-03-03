package com.muyi.core.log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模块包名注册表
 * <p>
 * 维护 包名前缀 → [moduleType, serverId] 的映射关系。
 * 由 AbstractGameModule 在 init 时自动注册，供 Logback Converter 查询。
 *
 * @author muyi
 */
public final class ModuleRegistry {

    private static final Map<String, String[]> REGISTRY = new ConcurrentHashMap<>();

    private ModuleRegistry() {}

    /**
     * 注册模块包名映射
     *
     * @param packagePrefix 包名前缀，如 "com.muyi.gate"
     * @param moduleType    模块类型，如 "gate"
     * @param serverId      服务器ID，如 "1002"
     */
    public static void register(String packagePrefix, String moduleType, String serverId) {
        REGISTRY.put(packagePrefix, new String[]{moduleType, serverId});
    }

    /**
     * 根据 Logger 名称（类全名）查找所属模块
     *
     * @return [moduleType, serverId] 或 null
     */
    public static String[] lookup(String loggerName) {
        for (var entry : REGISTRY.entrySet()) {
            if (loggerName.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
