package com.muyi.db.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指定实体类使用的工作线程索引
 * <p>
 * 用于将高频操作的表分散到不同线程，避免热点
 * 
 * <pre>{@code
 * @WorkerIndex(0)
 * public class PlayerEntity extends BaseEntity<PlayerEntity> {
 *     // ...
 * }
 * 
 * @WorkerIndex(1)
 * public class BuildingEntity extends BaseEntity<BuildingEntity> {
 *     // ...
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface WorkerIndex {
    /**
     * 工作线程索引（0 到 landThreads-1）
     */
    int value();
}
