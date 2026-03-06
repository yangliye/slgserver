package com.muyi.config.loader;

import com.muyi.config.IFieldConverter;
import com.muyi.config.annotation.ConfigConverter;

import java.lang.reflect.Field;

/**
 * 字段元数据（缓存反射信息，避免每次解析都反射）
 *
 * @author muyi
 */
class FieldMeta {

    final Field field;
    final String name;
    final Class<?> type;
    final Class<? extends IFieldConverter<?>> converterClass;

    FieldMeta(Field field) {
        field.setAccessible(true);
        this.field = field;
        this.name = field.getName();
        this.type = field.getType();

        ConfigConverter annotation = field.getAnnotation(ConfigConverter.class);
        this.converterClass = annotation != null ? annotation.value() : null;
    }
}
