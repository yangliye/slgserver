package com.muyi.game.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 英雄全局 Service（与玩家无关的业务逻辑）
 * <p>
 * Service 层处理不绑定到具体玩家的通用逻辑，例如：
 * <ul>
 *   <li>全服英雄排行榜</li>
 *   <li>英雄配置查询（配合配置模块）</li>
 *   <li>英雄相关的全局活动</li>
 * </ul>
 * <p>
 * Service 是单例，在模块初始化时创建；
 * Manager 是每个玩家一个实例，在登录时创建。
 *
 * @author muyi
 */
public class HeroService {

    private static final Logger log = LoggerFactory.getLogger(HeroService.class);
}
