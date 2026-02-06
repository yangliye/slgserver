package com.muyi.core.web;

import com.muyi.core.exception.BizException;
import com.muyi.core.web.annotation.GmApi;
import com.muyi.core.web.annotation.GmController;
import com.muyi.core.web.annotation.HttpMethod;
import com.muyi.core.web.annotation.Param;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GM 接口注册器
 * 自动扫描带 @GmApi 注解的方法并注册为路由
 *
 * @author muyi
 */
public class GmApiRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(GmApiRegistry.class);
    
    private final Javalin app;
    private final List<ApiInfo> registeredApis = new ArrayList<>();
    
    public GmApiRegistry(Javalin app) {
        this.app = app;
    }
    
    /**
     * 注册控制器
     * 
     * @param controller 控制器实例
     */
    public GmApiRegistry register(Object controller) {
        Class<?> clazz = controller.getClass();
        
        // 获取路径前缀
        String prefix = "";
        GmController controllerAnn = clazz.getAnnotation(GmController.class);
        if (controllerAnn != null) {
            prefix = controllerAnn.value();
        }
        
        // 扫描所有方法
        for (Method method : clazz.getDeclaredMethods()) {
            GmApi apiAnn = method.getAnnotation(GmApi.class);
            if (apiAnn == null) {
                continue;
            }
            
            String fullPath = prefix + apiAnn.path();
            registerMethod(controller, method, fullPath, apiAnn);
        }
        
        return this;
    }
    
    /**
     * 注册单个方法
     */
    private void registerMethod(Object controller, Method method, String path, GmApi apiAnn) {
        method.setAccessible(true);
        
        // 创建处理器
        io.javalin.http.Handler handler = ctx -> {
            try {
                Object result = invokeMethod(controller, method, ctx);
                
                // 如果方法返回 void 或已经处理了响应，跳过
                if (method.getReturnType() == void.class || ctx.res().isCommitted()) {
                    return;
                }
                
                // 自动包装返回值
                if (result != null) {
                    WebServer.success(ctx, result);
                } else {
                    WebServer.success(ctx, "OK");
                }
            } catch (BizException e) {
                WebServer.error(ctx, e.getCode(), e.getMessage());
            } catch (IllegalArgumentException e) {
                WebServer.error(ctx, 400, e.getMessage());
            } catch (Exception e) {
                log.error("API error: {} {}", apiAnn.method(), path, e);
                WebServer.error(ctx, 500, "服务器内部错误");
            }
        };
        
        // 注册路由
        switch (apiAnn.method()) {
            case GET -> app.get(path, handler);
            case POST -> app.post(path, handler);
            case PUT -> app.put(path, handler);
            case DELETE -> app.delete(path, handler);
        }
        
        // 记录 API 信息
        registeredApis.add(new ApiInfo(path, apiAnn.method(), apiAnn.description(), method));
        log.debug("Registered GM API: {} {} - {}", apiAnn.method(), path, apiAnn.description());
    }
    
    /**
     * 调用方法并自动注入参数
     */
    private Object invokeMethod(Object controller, Method method, Context ctx) throws Exception {
        Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];
        
        for (int i = 0; i < params.length; i++) {
            Parameter param = params[i];
            Class<?> type = param.getType();
            
            // 如果是 Context 类型，直接注入
            if (type == Context.class) {
                args[i] = ctx;
                continue;
            }
            
            // 检查 @Param 注解
            Param paramAnn = param.getAnnotation(Param.class);
            if (paramAnn != null) {
                args[i] = resolveParam(ctx, paramAnn, type);
            } else if (isSimpleType(type)) {
                // 简单类型：尝试用参数名从 query/form 获取
                String paramName = param.getName();
                args[i] = resolveParamByName(ctx, paramName, type, false, null);
            } else {
                // 复杂对象类型：从请求体 JSON 反序列化
                args[i] = resolveBodyParam(ctx, type);
            }
        }
        
        return method.invoke(controller, args);
    }
    
    /**
     * 判断是否为简单类型
     */
    private boolean isSimpleType(Class<?> type) {
        return type == String.class
                || type == int.class || type == Integer.class
                || type == long.class || type == Long.class
                || type == boolean.class || type == Boolean.class
                || type == double.class || type == Double.class
                || type == float.class || type == Float.class
                || type == short.class || type == Short.class
                || type == byte.class || type == Byte.class;
    }
    
    /**
     * 从请求体解析复杂对象参数
     */
    private Object resolveBodyParam(Context ctx, Class<?> type) {
        try {
            String body = ctx.body();
            if (body == null || body.isEmpty() || body.equals("{}")) {
                // 尝试创建空对象
                return type.getDeclaredConstructor().newInstance();
            }
            return ctx.bodyAsClass(type);
        } catch (Exception e) {
            log.warn("Failed to parse body as {}: {}", type.getSimpleName(), e.getMessage());
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                throw new IllegalArgumentException("无法解析请求体为: " + type.getSimpleName());
            }
        }
    }
    
    /**
     * 解析参数值
     */
    private Object resolveParam(Context ctx, Param paramAnn, Class<?> type) {
        String name = paramAnn.value();
        String value = ctx.queryParam(name);
        
        // 检查必填
        if (paramAnn.required() && (value == null || value.isEmpty())) {
            throw new IllegalArgumentException("缺少必填参数: " + name);
        }
        
        // 使用默认值
        if ((value == null || value.isEmpty()) && !paramAnn.defaultValue().isEmpty()) {
            value = paramAnn.defaultValue();
        }
        
        return convertValue(value, type, name);
    }
    
    /**
     * 按参数名解析
     */
    private Object resolveParamByName(Context ctx, String name, Class<?> type, 
                                       boolean required, String defaultValue) {
        String value = ctx.queryParam(name);
        
        if (required && (value == null || value.isEmpty()) && defaultValue == null) {
            throw new IllegalArgumentException("缺少必填参数: " + name);
        }
        
        if ((value == null || value.isEmpty()) && defaultValue != null) {
            value = defaultValue;
        }
        
        return convertValue(value, type, name);
    }
    
    /**
     * 类型转换
     */
    private Object convertValue(String value, Class<?> type, String paramName) {
        if (value == null || value.isEmpty()) {
            // 返回基本类型的默认值
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            if (type == boolean.class) return false;
            if (type == double.class) return 0.0;
            if (type == float.class) return 0.0f;
            return null;
        }
        
        try {
            if (type == String.class) {
                return value;
            } else if (type == int.class || type == Integer.class) {
                return Integer.parseInt(value);
            } else if (type == long.class || type == Long.class) {
                return Long.parseLong(value);
            } else if (type == boolean.class || type == Boolean.class) {
                return Boolean.parseBoolean(value);
            } else if (type == double.class || type == Double.class) {
                return Double.parseDouble(value);
            } else if (type == float.class || type == Float.class) {
                return Float.parseFloat(value);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("参数格式错误: " + paramName + "=" + value);
        }
        
        return value;
    }
    
    /**
     * 获取所有已注册的 API 信息
     */
    public List<ApiInfo> getRegisteredApis() {
        return registeredApis;
    }
    
    /**
     * 获取 API 列表（用于 GM 后台展示）
     */
    public List<Map<String, Object>> getApiList() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ApiInfo api : registeredApis) {
            Map<String, Object> map = new HashMap<>();
            map.put("path", api.path);
            map.put("method", api.method.name());
            map.put("desc", api.desc);
            map.put("params", extractParams(api.handler));
            list.add(map);
        }
        return list;
    }
    
    /**
     * 提取方法参数信息
     */
    private List<Map<String, Object>> extractParams(Method method) {
        List<Map<String, Object>> params = new ArrayList<>();
        for (Parameter param : method.getParameters()) {
            Class<?> type = param.getType();
            if (type == Context.class) {
                continue;
            }
            
            Map<String, Object> info = new HashMap<>();
            Param paramAnn = param.getAnnotation(Param.class);
            if (paramAnn != null) {
                info.put("name", paramAnn.value());
                info.put("required", paramAnn.required());
                info.put("defaultValue", paramAnn.defaultValue());
                info.put("source", "query");
            } else if (isSimpleType(type)) {
                info.put("name", param.getName());
                info.put("required", false);
                info.put("defaultValue", "");
                info.put("source", "query");
            } else {
                // 复杂对象类型
                info.put("name", param.getName());
                info.put("required", true);
                info.put("defaultValue", "");
                info.put("source", "body");
            }
            info.put("type", type.getSimpleName());
            params.add(info);
        }
        return params;
    }
    
    /**
     * API 信息
     */
    public record ApiInfo(String path, HttpMethod method, String desc, Method handler) {}
}
