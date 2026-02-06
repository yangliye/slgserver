package com.muyi.core.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 请求参数注解
 *
 * @author muyi
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Param {
    
    /**
     * 参数名
     */
    String value();
    
    /**
     * 是否必填
     */
    boolean required() default true;
    
    /**
     * 默认值（当非必填且未传时使用）
     */
    String defaultValue() default "";
}
