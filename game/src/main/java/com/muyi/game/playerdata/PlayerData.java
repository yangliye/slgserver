package com.muyi.game.playerdata;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个 {@link AbstractPlayerManager} 的实现类
 * <p>
 * 被标记的 Manager 会在玩家登录时自动实例化并加载数据
 *
 * @author muyi
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PlayerData {

    /**
     * 加载顺序（越小越先加载）
     * <p>
     * 有依赖关系的 Manager 需要按序加载，例如编队依赖英雄
     */
    int order() default 100;
}
