package com.muyi.db.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 列注解，标记实体字段对应的数据库列
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Column {
    
    /**
     * 列名（默认使用字段名的下划线形式）
     */
    String value() default "";
    
    /**
     * 是否可为空
     */
    boolean nullable() default true;
    
    /**
     * 列类型（用于自动建表）
     */
    String columnType() default "";
    
    /**
     * 默认值
     */
    String defaultValue() default "";
}
