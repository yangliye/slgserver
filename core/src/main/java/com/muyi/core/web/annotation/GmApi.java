package com.muyi.core.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * GM 接口注解
 * 标记在方法上，自动注册为 Web 路由
 *
 * @author muyi
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GmApi {
    
    /**
     * 路径，如 "/gate/status"
     */
    String path();
    
    /**
     * HTTP 方法
     */
    HttpMethod method() default HttpMethod.GET;
    
    /**
     * 接口描述
     */
    String description() default "";
}
