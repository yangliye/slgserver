package com.muyi.game.controller;

import com.muyi.core.web.annotation.GmApi;
import com.muyi.core.web.annotation.GmController;

import java.util.HashMap;
import java.util.Map;

/**
 * Game 模块 GM 控制器
 * 
 * 只提供基础状态查询，具体业务由各项目扩展实现
 *
 * @author muyi
 */
@GmController("/gm/game")
public class GameGmController {
    
    /**
     * 模块状态查询
     */
    @GmApi(path = "/status", description = "查询Game模块状态")
    public Map<String, Object> status() {
        Map<String, Object> result = new HashMap<>();
        result.put("module", "game");
        result.put("status", "running");
        // 具体业务指标由子类扩展
        return result;
    }
}
