package com.muyi.db.sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.muyi.db.core.BaseEntity;
import com.muyi.db.core.EntityMetadata;
import com.muyi.db.core.FieldInfo;

/**
 * SQL 构建器
 * <p>
 * 自动生成 INSERT/UPDATE/DELETE SQL，支持 SQL 模板缓存
 * </p>
 */
public class SqlBuilder {

    // ==================== SQL 缓存 ====================
    
    /**
     * SQL 模板缓存：class + sqlType + tableName -> SQL
     * <p>
     * tableName 作为 key 的一部分是为了支持动态表名（分表）
     */
    private static final ConcurrentHashMap<String, String> SQL_CACHE = new ConcurrentHashMap<>();
    
    private static String getCacheKey(Class<?> entityClass, String sqlType, String tableName) {
        return entityClass.getName() + "#" + sqlType + "#" + tableName;
    }

    // ==================== INSERT ====================

    /**
     * 构建 INSERT SQL（带缓存）
     */
    public static String buildInsert(BaseEntity<?> entity) {
        String tableName = entity.getTableName();
        String cacheKey = getCacheKey(entity.getClass(), "INSERT", tableName);
        
        return SQL_CACHE.computeIfAbsent(cacheKey, k -> {
            EntityMetadata metadata = entity.getMetadata();
            List<FieldInfo> fields = metadata.getAllFields();

            return "INSERT INTO " + tableName + " (" +
                    fields.stream().map(FieldInfo::getColumnName).collect(Collectors.joining(", ")) +
                    ") VALUES (" +
                    fields.stream().map(f -> "?").collect(Collectors.joining(", ")) +
                    ")";
        });
    }

    /**
     * 构建 INSERT SQL（排除自增主键，带缓存）
     */
    public static String buildInsertWithoutAutoIncrement(BaseEntity<?> entity) {
        String tableName = entity.getTableName();
        String cacheKey = getCacheKey(entity.getClass(), "INSERT_NO_AUTO", tableName);
        
        return SQL_CACHE.computeIfAbsent(cacheKey, k -> {
            EntityMetadata metadata = entity.getMetadata();
            List<FieldInfo> fields = metadata.getAllFields().stream()
                    .filter(f -> !f.isAutoIncrement())
                    .toList();

            return "INSERT INTO " + tableName + " (" +
                    fields.stream().map(FieldInfo::getColumnName).collect(Collectors.joining(", ")) +
                    ") VALUES (" +
                    fields.stream().map(f -> "?").collect(Collectors.joining(", ")) +
                    ")";
        });
    }

    /**
     * 构建批量 INSERT SQL
     */
    public static String buildBatchInsert(BaseEntity<?> entity, int batchSize) {
        EntityMetadata metadata = entity.getMetadata();
        String tableName = entity.getTableName();
        
        List<FieldInfo> fields = metadata.getAllFields();
        String columnsPart = fields.stream().map(FieldInfo::getColumnName).collect(Collectors.joining(", "));
        String valuesPart = "(" + fields.stream().map(f -> "?").collect(Collectors.joining(", ")) + ")";
        
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(tableName).append(" (");
        sql.append(columnsPart);
        sql.append(") VALUES ");
        
        for (int i = 0; i < batchSize; i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(valuesPart);
        }
        
        return sql.toString();
    }

    /**
     * 构建 INSERT ON DUPLICATE KEY UPDATE SQL（带缓存）
     */
    public static String buildUpsert(BaseEntity<?> entity) {
        String tableName = entity.getTableName();
        String cacheKey = getCacheKey(entity.getClass(), "UPSERT", tableName);
        
        return SQL_CACHE.computeIfAbsent(cacheKey, k -> {
            EntityMetadata metadata = entity.getMetadata();
            List<FieldInfo> fields = metadata.getAllFields();
            List<FieldInfo> nonPkFields = fields.stream()
                    .filter(f -> !f.isPrimaryKey())
                    .toList();

            return "INSERT INTO " + tableName + " (" +
                    fields.stream().map(FieldInfo::getColumnName).collect(Collectors.joining(", ")) +
                    ") VALUES (" +
                    fields.stream().map(f -> "?").collect(Collectors.joining(", ")) +
                    ") ON DUPLICATE KEY UPDATE " +
                    nonPkFields.stream()
                            .map(f -> f.getColumnName() + " = VALUES(" + f.getColumnName() + ")")
                            .collect(Collectors.joining(", "));
        });
    }

    // ==================== UPDATE ====================

    /**
     * 构建 UPDATE SQL（更新所有字段，带缓存）
     */
    public static String buildUpdate(BaseEntity<?> entity) {
        String tableName = entity.getTableName();
        String cacheKey = getCacheKey(entity.getClass(), "UPDATE", tableName);
        
        return SQL_CACHE.computeIfAbsent(cacheKey, k -> {
            EntityMetadata metadata = entity.getMetadata();
            List<FieldInfo> nonPkFields = metadata.getAllFields().stream()
                    .filter(f -> !f.isPrimaryKey())
                    .toList();
            
            List<FieldInfo> pkFields = metadata.getPrimaryKeys();

            return "UPDATE " + tableName + " SET " +
                    nonPkFields.stream()
                            .map(f -> f.getColumnName() + " = ?")
                            .collect(Collectors.joining(", ")) +
                    " WHERE " +
                    pkFields.stream()
                            .map(f -> f.getColumnName() + " = ?")
                            .collect(Collectors.joining(" AND "));
        });
    }

    /**
     * 构建 UPDATE SQL（只更新变更字段）
     */
    public static String buildPartialUpdate(BaseEntity<?> entity) {
        EntityMetadata metadata = entity.getMetadata();
        String tableName = entity.getTableName();
        Set<String> changedFields = entity.getChangedFields();
        
        if (changedFields.isEmpty()) {
            return null;
        }
        
        List<FieldInfo> updateFields = metadata.getAllFields().stream()
                .filter(f -> changedFields.contains(f.getFieldName()) && !f.isPrimaryKey())
                .toList();
        
        if (updateFields.isEmpty()) {
            return null;
        }
        
        List<FieldInfo> pkFields = metadata.getPrimaryKeys();
        
        return "UPDATE " + tableName + " SET " +
                updateFields.stream()
                        .map(f -> f.getColumnName() + " = ?")
                        .collect(Collectors.joining(", ")) +
                " WHERE " +
                pkFields.stream()
                        .map(f -> f.getColumnName() + " = ?")
                        .collect(Collectors.joining(" AND "));
    }

    /**
     * 获取部分更新的参数值
     */
    public static Object[] getPartialUpdateValues(BaseEntity<?> entity) {
        EntityMetadata metadata = entity.getMetadata();
        Set<String> changedFields = entity.getChangedFields();
        List<FieldInfo> primaryKeys = metadata.getPrimaryKeys();
        
        // 预分配容量：变更字段数 + 主键字段数
        List<Object> values = new ArrayList<>(changedFields.size() + primaryKeys.size());
        
        // 变更字段的值
        for (FieldInfo field : metadata.getAllFields()) {
            if (changedFields.contains(field.getFieldName()) && !field.isPrimaryKey()) {
                values.add(field.getValue(entity));
            }
        }
        
        // 主键值
        for (FieldInfo pk : primaryKeys) {
            values.add(pk.getValue(entity));
        }
        
        return values.toArray();
    }

    // ==================== DELETE ====================

    /**
     * 构建 DELETE SQL（带缓存）
     */
    public static String buildDelete(BaseEntity<?> entity) {
        String tableName = entity.getTableName();
        String cacheKey = getCacheKey(entity.getClass(), "DELETE", tableName);
        
        return SQL_CACHE.computeIfAbsent(cacheKey, k -> {
            EntityMetadata metadata = entity.getMetadata();
            List<FieldInfo> pkFields = metadata.getPrimaryKeys();
            
            return "DELETE FROM " + tableName + " WHERE " +
                    pkFields.stream()
                            .map(f -> f.getColumnName() + " = ?")
                            .collect(Collectors.joining(" AND "));
        });
    }

    /**
     * 构建批量 DELETE SQL
     */
    public static String buildBatchDelete(BaseEntity<?> entity, int batchSize) {
        EntityMetadata metadata = entity.getMetadata();
        String tableName = entity.getTableName();
        List<FieldInfo> pkFields = metadata.getPrimaryKeys();
        
        // 单主键使用 IN，复合主键使用 OR
        if (pkFields.size() == 1) {
            return "DELETE FROM " + tableName + " WHERE " +
                    pkFields.get(0).getColumnName() + " IN (" +
                    String.join(", ", Collections.nCopies(batchSize, "?")) + ")";
        } else {
            String pkCondition = "(" + pkFields.stream()
                    .map(f -> f.getColumnName() + " = ?")
                    .collect(Collectors.joining(" AND ")) + ")";
            return "DELETE FROM " + tableName + " WHERE " +
                    String.join(" OR ", Collections.nCopies(batchSize, pkCondition));
        }
    }

    // ==================== SELECT ====================

    /**
     * 构建 SELECT BY PRIMARY KEY SQL（带缓存）
     */
    public static String buildSelectByPrimaryKey(BaseEntity<?> entity) {
        String tableName = entity.getTableName();
        String cacheKey = getCacheKey(entity.getClass(), "SELECT_PK", tableName);
        
        return SQL_CACHE.computeIfAbsent(cacheKey, k -> {
            EntityMetadata metadata = entity.getMetadata();
            List<FieldInfo> pkFields = metadata.getPrimaryKeys();
            
            return "SELECT " + String.join(", ", metadata.getColumnNames()) +
                    " FROM " + tableName + " WHERE " +
                    pkFields.stream()
                            .map(f -> f.getColumnName() + " = ?")
                            .collect(Collectors.joining(" AND "));
        });
    }

    /**
     * 构建 SELECT BY 条件 SQL
     */
    public static String buildSelectByCondition(BaseEntity<?> entity, Map<String, Object> conditions) {
        EntityMetadata metadata = entity.getMetadata();
        String tableName = entity.getTableName();
        
        String sql = "SELECT " + String.join(", ", metadata.getColumnNames()) + " FROM " + tableName;
        
        if (conditions != null && !conditions.isEmpty()) {
            sql += " WHERE " + conditions.keySet().stream()
                    .map(k -> {
                        FieldInfo field = metadata.getField(k);
                        String columnName = field != null ? field.getColumnName() : k;
                        return columnName + " = ?";
                    })
                    .collect(Collectors.joining(" AND "));
        }
        
        return sql;
    }

    // ==================== 参数提取 ====================

    /**
     * 获取 INSERT 参数值
     */
    public static Object[] getInsertValues(BaseEntity<?> entity) {
        return entity.getAllValues();
    }

    /**
     * 获取 INSERT 参数值（排除自增主键）
     */
    public static Object[] getInsertValuesWithoutAutoIncrement(BaseEntity<?> entity) {
        EntityMetadata metadata = entity.getMetadata();
        return metadata.getAllFields().stream()
                .filter(f -> !f.isAutoIncrement())
                .map(f -> f.getValue(entity))
                .toArray();
    }

    /**
     * 获取 UPDATE 参数值（非主键字段 + 主键字段）
     */
    public static Object[] getUpdateValues(BaseEntity<?> entity) {
        EntityMetadata metadata = entity.getMetadata();
        List<FieldInfo> allFields = metadata.getAllFields();
        List<FieldInfo> primaryKeys = metadata.getPrimaryKeys();
        
        // 预分配容量：非主键字段数 + 主键字段数
        List<Object> values = new ArrayList<>(allFields.size());
        
        // 非主键字段的值
        for (FieldInfo field : allFields) {
            if (!field.isPrimaryKey()) {
                values.add(field.getValue(entity));
            }
        }
        
        // 主键值
        for (FieldInfo pk : primaryKeys) {
            values.add(pk.getValue(entity));
        }
        
        return values.toArray();
    }

    /**
     * 获取 DELETE 参数值（主键值）
     */
    public static Object[] getDeleteValues(BaseEntity<?> entity) {
        return entity.getPrimaryKeyValues();
    }
}
