package com.muyi.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 配置文件注解
 * 标记配置类对应的文件名
 *
 * @author muyi
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigFile {

    /**
     * 配置文件名（相对于 configRoot）
     * 如果为空，则使用类名生成
     */
    String value() default "";
}
