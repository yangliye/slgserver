package com.muyi.db.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 主键注解
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PrimaryKey {
    
    /**
     * 是否自增
     */
    boolean autoIncrement() default false;
    
    /**
     * 主键顺序（复合主键时使用）
     */
    int order() default 0;
}
