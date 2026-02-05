package com.muyi.db.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 索引注解
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(Indexes.class)
public @interface Index {
    
    /**
     * 索引名称
     */
    String name() default "";
    
    /**
     * 是否唯一索引
     */
    boolean unique() default false;
    
    /**
     * 复合索引中的顺序
     */
    int order() default 0;
}
