package com.muyi.rpc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RPC 方法超时注解
 * 用于为特定方法设置自定义超时时间
 * 
 * 使用示例:
 * <pre>
 * public interface IWorldService {
 *     
 *     // 使用默认超时
 *     WorldInfo getWorldInfo(int worldId);
 *     
 *     // 复杂查询，需要更长超时
 *     RpcTimeout(10000)
 *     List<WorldInfo> searchWorlds(String keyword);
 *     
 *     // 耗时操作
 *     RpcTimeout(value = 30000, retries = 0)
 *     void syncWorldData(int worldId);
 * }
 * </pre>
 *
 * @author muyi
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcTimeout {
    
    /**
     * 超时时间（毫秒）
     */
    long value() default 5000;
    
    /**
     * 重试次数（-1 表示使用默认值）
     */
    int retries() default -1;
}

