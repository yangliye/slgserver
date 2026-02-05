package com.muyi.db.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表注解，标记实体对应的数据库表
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Table {
    
    /**
     * 表名
     */
    String value();
    
    /**
     * 分表策略键（如 uid, serverId）
     */
    String shardKey() default "";
    
    /**
     * 分表数量（0表示不分表）
     */
    int shardCount() default 0;
}
