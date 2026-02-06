package com.muyi.gameconfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 配置文件注解
 * 标记配置类对应的文件名
 * 
 * 使用示例：
 * <pre>{@code
 * @ConfigFile("unit.xml")
 * public class UnitConfig implements IConfig {
 *     // ...
 * }
 * }</pre>
 *
 * @author muyi
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigFile {
    
    /**
     * 配置文件名（相对于 configRoot）
     * 如果为空，则使用类名小写 + .xml
     * 例如：UnitConfig -> unit.xml
     */
    String value() default "";
}
