package com.muyi.db.core;

import java.lang.reflect.Field;

import com.muyi.db.annotation.Column;
import com.muyi.db.annotation.PrimaryKey;
import com.muyi.db.util.StringUtils;

/**
 * 字段信息
 * <p>
 * 封装实体字段的元数据，包括字段名、列名、类型、主键信息等
 */
public class FieldInfo {
    private final Field field;
    private final String fieldName;
    private final String columnName;
    private final Class<?> fieldType;
    private final boolean primaryKey;
    private final int primaryKeyOrder;
    private final boolean autoIncrement;
    private final boolean nullable;

    public FieldInfo(Field field) {
        this.field = field;
        this.fieldName = field.getName();
        this.fieldType = field.getType();

        // 解析 @Column 注解
        Column columnAnn = field.getAnnotation(Column.class);
        if (columnAnn != null && !columnAnn.value().isEmpty()) {
            this.columnName = columnAnn.value();
        } else {
            this.columnName = StringUtils.camelToSnake(fieldName);
        }
        this.nullable = columnAnn == null || columnAnn.nullable();

        // 解析 @PrimaryKey 注解
        PrimaryKey pkAnn = field.getAnnotation(PrimaryKey.class);
        this.primaryKey = pkAnn != null;
        this.primaryKeyOrder = pkAnn != null ? pkAnn.order() : 0;
        this.autoIncrement = pkAnn != null && pkAnn.autoIncrement();
    }

    public Object getValue(Object entity) {
        try {
            return field.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to get field value: " + fieldName, e);
        }
    }

    public void setValue(Object entity, Object value) {
        try {
            field.set(entity, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to set field value: " + fieldName, e);
        }
    }

    // Getters
    public String getFieldName() { return fieldName; }
    public String getColumnName() { return columnName; }
    public Class<?> getFieldType() { return fieldType; }
    public boolean isPrimaryKey() { return primaryKey; }
    public int getPrimaryKeyOrder() { return primaryKeyOrder; }
    public boolean isAutoIncrement() { return autoIncrement; }
    public boolean isNullable() { return nullable; }
}
