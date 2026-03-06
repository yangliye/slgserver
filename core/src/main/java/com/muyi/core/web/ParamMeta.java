package com.muyi.core.web;

import com.muyi.core.web.annotation.Param;
import io.javalin.http.Context;

import java.lang.reflect.Parameter;

/**
 * 参数元数据（注册时解析，运行时复用）
 *
 * @author muyi
 */
class ParamMeta {
    
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
