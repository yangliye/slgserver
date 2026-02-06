package com.muyi.core.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * GM 控制器注解
 * 标记在类上，表示这是一个 GM 接口控制器
 *
 * @author muyi
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GmController {
    
    /**
     * 路径前缀，如 "/gm/gate"
     */
    String value() default "";
}
