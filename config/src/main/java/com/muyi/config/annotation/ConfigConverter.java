package com.muyi.config.annotation;

import com.muyi.config.IFieldConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 配置字段转换器注解
 * 用于标记需要自定义解析的字段
 *
 * @author muyi
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigConverter {

    /**
     * 转换器类
     */
    Class<? extends IFieldConverter<?>> value();
}
