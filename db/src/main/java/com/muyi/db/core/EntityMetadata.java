package com.muyi.db.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.muyi.db.annotation.Table;
import com.muyi.db.util.StringUtils;

/**
 * 实体元数据
 * <p>
 * 解析实体类的注解信息，缓存字段映射关系
 * </p>
 */
public class EntityMetadata {

    private final Class<?> entityClass;
    private final String tableName;
    private final String shardKey;
    private final int shardCount;

    /**
     * 所有字段（按声明顺序）
     */
    private final List<FieldInfo> allFields = new ArrayList<>();

    /**
     * 主键字段
     */
    private final List<FieldInfo> primaryKeys = new ArrayList<>();

    /**
     * 字段名 -> FieldInfo 映射
     */
    private final Map<String, FieldInfo> fieldMap = new LinkedHashMap<>();

    /**
     * 列名 -> FieldInfo 映射
     */
    private final Map<String, FieldInfo> columnMap = new LinkedHashMap<>();

    public EntityMetadata(Class<?> entityClass) {
        this.entityClass = entityClass;

        // 解析 @Table 注解
        Table tableAnn = entityClass.getAnnotation(Table.class);
        if (tableAnn != null) {
            this.tableName = tableAnn.value();
            this.shardKey = tableAnn.shardKey();
            this.shardCount = tableAnn.shardCount();
        } else {
            this.tableName = StringUtils.camelToSnake(entityClass.getSimpleName());
            this.shardKey = "";
            this.shardCount = 0;
        }

        // 解析字段
        parseFields(entityClass);
    }

    private void parseFields(Class<?> clazz) {
        // 递归处理父类字段
        if (clazz.getSuperclass() != null && clazz.getSuperclass() != BaseEntity.class) {
            parseFields(clazz.getSuperclass());
        }

        for (Field field : clazz.getDeclaredFields()) {
            // 跳过 transient 和 static 字段
            if (java.lang.reflect.Modifier.isTransient(field.getModifiers()) ||
                java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            field.setAccessible(true);

            FieldInfo fieldInfo = new FieldInfo(field);
            allFields.add(fieldInfo);
            fieldMap.put(fieldInfo.getFieldName(), fieldInfo);
            columnMap.put(fieldInfo.getColumnName(), fieldInfo);

            if (fieldInfo.isPrimaryKey()) {
                primaryKeys.add(fieldInfo);
            }
        }

        // 按主键顺序排序
        primaryKeys.sort(Comparator.comparingInt(FieldInfo::getPrimaryKeyOrder));
    }

    // ==================== Getter ====================

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public String getTableName() {
        return tableName;
    }

    public String getShardKey() {
        return shardKey;
    }

    public int getShardCount() {
        return shardCount;
    }

    public List<FieldInfo> getAllFields() {
        return Collections.unmodifiableList(allFields);
    }

    public List<FieldInfo> getPrimaryKeys() {
        return Collections.unmodifiableList(primaryKeys);
    }

    public FieldInfo getField(String fieldName) {
        return fieldMap.get(fieldName);
    }

    public FieldInfo getFieldByColumn(String columnName) {
        return columnMap.get(columnName);
    }

    // ==================== 值操作 ====================

    public Object[] getPrimaryKeyValues(Object entity) {
        Object[] values = new Object[primaryKeys.size()];
        for (int i = 0; i < primaryKeys.size(); i++) {
            values[i] = primaryKeys.get(i).getValue(entity);
        }
        return values;
    }

    public Object[] getAllValues(Object entity) {
        Object[] values = new Object[allFields.size()];
        for (int i = 0; i < allFields.size(); i++) {
            values[i] = allFields.get(i).getValue(entity);
        }
        return values;
    }

    public Object getFieldValue(Object entity, String fieldName) {
        FieldInfo fieldInfo = fieldMap.get(fieldName);
        return fieldInfo != null ? fieldInfo.getValue(entity) : null;
    }

    public void setFieldValue(Object entity, String fieldName, Object value) {
        FieldInfo fieldInfo = fieldMap.get(fieldName);
        if (fieldInfo != null) {
            fieldInfo.setValue(entity, value);
        }
    }

    public void setValues(Object entity, Map<String, Object> values) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            // 尝试按字段名匹配
            FieldInfo fieldInfo = fieldMap.get(entry.getKey());
            if (fieldInfo == null) {
                // 尝试按列名匹配
                fieldInfo = columnMap.get(entry.getKey());
            }
            if (fieldInfo != null) {
                fieldInfo.setValue(entity, convertValue(entry.getValue(), fieldInfo.getFieldType()));
            }
        }
    }

    public Map<String, Object> toMap(Object entity) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (FieldInfo fieldInfo : allFields) {
            map.put(fieldInfo.getFieldName(), fieldInfo.getValue(entity));
        }
        return map;
    }

    // ==================== SQL 相关 ====================

    public String[] getColumnNames() {
        String[] names = new String[allFields.size()];
        for (int i = 0; i < allFields.size(); i++) {
            names[i] = allFields.get(i).getColumnName();
        }
        return names;
    }

    public String[] getPrimaryKeyColumnNames() {
        String[] names = new String[primaryKeys.size()];
        for (int i = 0; i < primaryKeys.size(); i++) {
            names[i] = primaryKeys.get(i).getColumnName();
        }
        return names;
    }

    public String[] getNonPrimaryKeyColumnNames() {
        List<String> names = new ArrayList<>();
        for (FieldInfo field : allFields) {
            if (!field.isPrimaryKey()) {
                names.add(field.getColumnName());
            }
        }
        return names.toArray(new String[0]);
    }

    // ==================== 工具方法 ====================

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType.isInstance(value)) {
            return value;
        }

        String strValue = value.toString();

        if (targetType == int.class || targetType == Integer.class) {
            return strValue.isEmpty() ? 0 : Integer.parseInt(strValue);
        } else if (targetType == long.class || targetType == Long.class) {
            return strValue.isEmpty() ? 0L : Long.parseLong(strValue);
        } else if (targetType == double.class || targetType == Double.class) {
            return strValue.isEmpty() ? 0.0 : Double.parseDouble(strValue);
        } else if (targetType == float.class || targetType == Float.class) {
            return strValue.isEmpty() ? 0.0f : Float.parseFloat(strValue);
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return "1".equals(strValue) || "true".equalsIgnoreCase(strValue);
        } else if (targetType == String.class) {
            return strValue;
        }

        return value;
    }

}
