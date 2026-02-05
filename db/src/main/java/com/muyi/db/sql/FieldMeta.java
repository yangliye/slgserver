package com.muyi.db.sql;

import java.util.ArrayList;
import java.util.List;

/**
 * 字段元数据（用于表生成）
 */
class FieldMeta {
    String fieldName;
    Class<?> fieldType;
    String columnName;
    String columnType = "";
    boolean nullable = true;
    String defaultValue = "";
    boolean primaryKey = false;
    boolean autoIncrement = false;
    List<IndexMeta> indexes = new ArrayList<>();
}
