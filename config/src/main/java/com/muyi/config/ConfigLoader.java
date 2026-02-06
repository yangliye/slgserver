package com.muyi.config;

import java.util.List;

/**
 * 配置加载器接�?
 *
 * @author muyi
 */
public interface ConfigLoader {
    
    /**
     * 加载配置
     *
     * @param path 配置文件路径
     * @param configClass 配置类型
     * @return 配置列表
     */
    <T extends IConfig> List<T> load(String path, Class<T> configClass) throws Exception;
    
    /**
     * 支持的文件扩展名
     */
    String[] supportedExtensions();
}
