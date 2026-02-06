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
    
    /** 参数元数据缓存：Method -> ParamMeta[] */
    private final Map<Method, ParamMeta[]> paramMetaCache = new HashMap<>();
    
    public GmApiRegistry(Javalin app) {
        this.app = app;
    }
    
    /**
     * 参数元数据（注册时解析，运行时复用）
     */
    private static class ParamMeta {
        final Class<?> type;
        final String name;
        final boolean required;
        final String defaultValue;
        final boolean isContext;
        final boolean isSimpleType;
        final boolean hasParamAnnotation;
        
        ParamMeta(Parameter param) {
            this.type = param.getType();
            this.isContext = type == Context.class;
            this.isSimpleType = checkSimpleType(type);
            
            Param paramAnn = param.getAnnotation(Param.class);
            this.hasParamAnnotation = paramAnn != null;
            
            if (paramAnn != null) {
                this.name = paramAnn.value();
                this.required = paramAnn.required();
                this.defaultValue = paramAnn.defaultValue();
            } else {
                this.name = param.getName();
                this.required = false;
                this.defaultValue = "";
            }
        }
        
        private static boolean checkSimpleType(Class<?> type) {
            return type == String.class
                    || type == int.class || type == Integer.class
                    || type == long.class || type == Long.class
                    || type == boolean.class || type == Boolean.class
                    || type == double.class || type == Double.class
                    || type == float.class || type == Float.class
                    || type == short.class || type == Short.class
                    || type == byte.class || type == Byte.class;
        }
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
        
        // 预解析参数元数据（只在注册时执行一次）
        Parameter[] params = method.getParameters();
        ParamMeta[] paramMetas = new ParamMeta[params.length];
        for (int i = 0; i < params.length; i++) {
            paramMetas[i] = new ParamMeta(params[i]);
        }
        paramMetaCache.put(method, paramMetas);
        
        // 创建处理器
        io.javalin.http.Handler handler = ctx -> {
            try {
                Object result = invokeMethod(controller, method, paramMetas, ctx);
                
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
     * 调用方法并自动注入参数（使用缓存的元数据）
     */
    private Object invokeMethod(Object controller, Method method, ParamMeta[] paramMetas, Context ctx) throws Exception {
        Object[] args = new Object[paramMetas.length];
        
        for (int i = 0; i < paramMetas.length; i++) {
            ParamMeta meta = paramMetas[i];
            
            // 如果是 Context 类型，直接注入
            if (meta.isContext) {
                args[i] = ctx;
                continue;
            }
            
            // 根据参数元数据解析
            if (meta.hasParamAnnotation) {
                args[i] = resolveParamWithMeta(ctx, meta);
            } else if (meta.isSimpleType) {
                // 简单类型：尝试用参数名从 query/form 获取
                args[i] = resolveParamByName(ctx, meta.name, meta.type, false, null);
            } else {
                // 复杂对象类型：从请求体 JSON 反序列化
                args[i] = resolveBodyParam(ctx, meta.type);
            }
        }
        
        return method.invoke(controller, args);
    }
    
    /**
     * 使用参数元数据解析参数值
     */
    private Object resolveParamWithMeta(Context ctx, ParamMeta meta) {
        String value = ctx.queryParam(meta.name);
        
        // 检查必填
        if (meta.required && (value == null || value.isEmpty())) {
            throw new IllegalArgumentException("缺少必填参数: " + meta.name);
        }
        
        // 使用默认值
        if ((value == null || value.isEmpty()) && !meta.defaultValue.isEmpty()) {
            value = meta.defaultValue;
        }
        
        return convertValue(value, meta.type, meta.name);
    }
    
    /**
     * 从请求体解析复杂对象参数
     */
    private Object resolveBodyParam(Context ctx, Class<?> type) {
        String body = ctx.body();
        
        // 空请求体直接创建默认对象
        if (body == null || body.isEmpty() || body.equals("{}")) {
            return createDefaultInstance(type);
        }
        
        // 尝试 JSON 反序列化
        try {
            return ctx.bodyAsClass(type);
        } catch (Exception e) {
            log.warn("Failed to parse body as {}: {}", type.getSimpleName(), e.getMessage());
            return createDefaultInstance(type);
        }
    }
    
    /**
     * 创建默认实例
     */
    private Object createDefaultInstance(Class<?> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("无法创建默认实例: " + type.getSimpleName());
        }
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
            if (type == short.class) return (short) 0;
            if (type == byte.class) return (byte) 0;
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
            } else if (type == short.class || type == Short.class) {
                return Short.parseShort(value);
            } else if (type == byte.class || type == Byte.class) {
                return Byte.parseByte(value);
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
     * 提取方法参数信息（使用缓存的元数据）
     */
    private List<Map<String, Object>> extractParams(Method method) {
        List<Map<String, Object>> params = new ArrayList<>();
        ParamMeta[] metas = paramMetaCache.get(method);
        if (metas == null) {
            return params;
        }
        
        for (ParamMeta meta : metas) {
            if (meta.isContext) {
                continue;
            }
            
            Map<String, Object> info = new HashMap<>();
            info.put("name", meta.name);
            info.put("type", meta.type.getSimpleName());
            
            if (meta.hasParamAnnotation || meta.isSimpleType) {
                info.put("required", meta.required);
                info.put("defaultValue", meta.defaultValue);
                info.put("source", "query");
            } else {
                info.put("required", true);
                info.put("defaultValue", "");
                info.put("source", "body");
            }
            params.add(info);
        }
        return params;
    }
    
    /**
     * API 信息
     */
    public record ApiInfo(String path, HttpMethod method, String desc, Method handler) {}
}
