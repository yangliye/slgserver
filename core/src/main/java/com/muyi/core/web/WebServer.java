package com.muyi.core.web;

import com.alibaba.fastjson2.JSON;
import com.muyi.common.util.time.TimeUtils;
import com.muyi.core.exception.BizException;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.plugin.bundled.CorsPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Web 服务器封装
 * 基于 Javalin，用于 GM 后台接口
 *
 * @author muyi
 */
public class WebServer {
    
    private static final Logger log = LoggerFactory.getLogger(WebServer.class);
    
    private final int port;
    private final Javalin app;
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    /** 复用的 API 注册器实例（volatile 保证可见性） */
    private volatile GmApiRegistry registry;
    
    public WebServer(int port) {
        this.port = port;
        this.app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            // 启用 CORS
            config.bundledPlugins.enableCors(cors -> cors.addRule(CorsPluginConfig.CorsRule::anyHost));
        });
        
        // 业务异常处理（不打印堆栈，属于正常业务流程）
        app.exception(BizException.class, (e, ctx) -> {
            log.warn("Biz exception on {}: [{}] {}", ctx.path(), e.getCode(), e.getMessage());
            error(ctx, e.getCode(), e.getMessage());
        });
        
        // 全局异常处理（打印堆栈，属于意外错误）
        app.exception(Exception.class, (e, ctx) -> {
            log.error("Web request error: {}", ctx.path(), e);
            error(ctx, 500, "Internal Server Error: " + e.getMessage());
        });
        
        // 默认健康检查接口
        app.get("/health", ctx -> success(ctx, Map.of(
                "status", "UP",
                "port", port,
                "timestamp", TimeUtils.currentTimeMillis()
        )));
        
        // API 列表接口（用于查看所有已注册的 GM 接口）
        app.get("/api-list", ctx -> {
            if (registry != null) {
                success(ctx, registry.getApiList());
            } else {
                success(ctx, List.of());
            }
        });
    }
    
    /**
     * 启动 Web 服务
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            app.start(port);
            log.info("Web server started on port {}", port);
        }
    }
    
    /**
     * 停止 Web 服务
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            app.stop();
            log.info("Web server stopped");
        }
    }
    
    /**
     * 是否运行中
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * 获取端口
     */
    public int getPort() {
        return port;
    }
    
    // ==================== 路由注册 ====================
    
    /**
     * GET 请求
     */
    public WebServer get(String path, Handler handler) {
        app.get(path, handler);
        return this;
    }
    
    /**
     * POST 请求
     */
    public WebServer post(String path, Handler handler) {
        app.post(path, handler);
        return this;
    }
    
    /**
     * PUT 请求
     */
    public WebServer put(String path, Handler handler) {
        app.put(path, handler);
        return this;
    }
    
    /**
     * DELETE 请求
     */
    public WebServer delete(String path, Handler handler) {
        app.delete(path, handler);
        return this;
    }
    
    /**
     * 获取 Javalin 实例（用于高级配置）
     */
    public Javalin getApp() {
        return app;
    }
    
    /**
     * 获取 API 注册器（双重检查锁定，线程安全懒加载）
     * 用于注解式路由注册
     */
    public GmApiRegistry getRegistry() {
        if (registry == null) {
            synchronized (this) {
                if (registry == null) {
                    registry = new GmApiRegistry(app);
                }
            }
        }
        return registry;
    }
    
    /**
     * 注册控制器（快捷方法）
     * 复用同一个 GmApiRegistry 实例
     */
    public WebServer registerController(Object... controllers) {
        for (Object controller : controllers) {
            getRegistry().register(controller);
        }
        return this;
    }
    
    // ==================== 响应工具方法 ====================
    
    /**
     * 成功响应
     */
    public static void success(Context ctx, Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "success");
        result.put("data", data);
        ctx.contentType("application/json");
        ctx.result(JSON.toJSONString(result));
    }
    
    /**
     * 成功响应（无数据）
     */
    public static void success(Context ctx) {
        success(ctx, null);
    }
    
    /**
     * 错误响应
     */
    public static void error(Context ctx, int code, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        result.put("data", null);
        ctx.contentType("application/json");
        ctx.status(code >= 400 && code < 600 ? code : 200);
        ctx.result(JSON.toJSONString(result));
    }
    
    /**
     * 业务错误响应
     */
    public static void fail(Context ctx, String message) {
        error(ctx, 1, message);
    }
}
