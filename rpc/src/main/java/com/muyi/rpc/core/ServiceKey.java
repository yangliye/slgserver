package com.muyi.rpc.core;

/**
 * 服务唯一标识生成工具
 * 
 * 格式: interfaceName#serverId
 * 示例: com.example.IWorldService#1  (World服务，实例1)
 *
 * @author muyi
 */
public class ServiceKey {
    
    private static final String SEPARATOR = "#";
    
    /**
     * 构建服务唯一标识
     *
     * @param interfaceName 接口名
     * @param serverId      服务器ID（0 表示不指定，由负载均衡选择）
     * @return 服务唯一标识
     */
    public static String build(String interfaceName, int serverId) {
        return interfaceName + SEPARATOR + serverId;
    }
    
    /**
     * 获取接口名前缀（用于服务发现时匹配所有同类型服务）
     *
     * @param interfaceName 接口名
     * @return 接口名前缀
     */
    public static String buildPrefix(String interfaceName) {
        return interfaceName + SEPARATOR;
    }
    
    /**
     * 解析服务标识
     *
     * @param serviceKey 服务标识
     * @return [interfaceName, serverId]，如果 serviceKey 为 null 返回空数组
     */
    public static String[] parse(String serviceKey) {
        if (serviceKey == null) {
            return new String[0];
        }
        return serviceKey.split(SEPARATOR);
    }
    
    /**
     * 从 serviceKey 解析 serverId
     *
     * @param serviceKey 服务标识
     * @return serverId，解析失败返回 0
     */
    public static int parseServerId(String serviceKey) {
        if (serviceKey == null) {
            return 0;
        }
        String[] parts = parse(serviceKey);
        if (parts.length >= 2) {
            try {
                return Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
}

