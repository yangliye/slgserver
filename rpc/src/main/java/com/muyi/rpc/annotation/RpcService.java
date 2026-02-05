package com.muyi.rpc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RPC 服务注解 - 标注在服务实现类上
 * 用于将服务暴露给远程调用
 * 
 * 使用示例:
 * <pre>
 * {@literal @}RpcService(IGameService.class)
 * public class GameService implements IGameService { ... }
 * 
 * {@literal @}RpcService(IWorldService.class)
 * public class WorldService implements IWorldService { ... }
 * </pre>
 *
 * @author muyi
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcService {
    
    /**
     * 服务接口类
     * 如果未指定则自动使用实现的第一个接口
     */
    Class<?> value() default void.class;
}
