package com.muyi.db.sql;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.muyi.db.annotation.Column;
import com.muyi.db.annotation.Index;
import com.muyi.db.annotation.Indexes;
import com.muyi.db.annotation.PrimaryKey;
import com.muyi.db.annotation.Table;
import com.muyi.db.core.BaseEntity;
import com.muyi.db.util.StringUtils;

/**
 * SQL 建表脚本生成器
 * <p>
 * 根据实体类注解生成 CREATE TABLE SQL
 * </p>
 */
public class TableGenerator {

    /**
     * 生成建表 SQL
     *
     * @param entityClass 实体类
     * @return CREATE TABLE SQL
     */
    public static String generateCreateTable(Class<? extends BaseEntity<?>> entityClass) {
        String tableName = getTableName(entityClass);
        List<FieldMeta> fields = parseFields(entityClass);
        
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS `").append(tableName).append("` (\n");
        
        // 字段定义
        List<String> columnDefs = new ArrayList<>();
        List<String> pkColumns = new ArrayList<>();
        Map<String, List<IndexMeta>> indexMap = new LinkedHashMap<>();
        
        for (FieldMeta field : fields) {
            columnDefs.add(buildColumnDefinition(field));
            
            if (field.primaryKey) {
                pkColumns.add("`" + field.columnName + "`");
            }
            
            // 收集索引
            for (IndexMeta idx : field.indexes) {
                indexMap.computeIfAbsent(idx.name, k -> new ArrayList<>()).add(idx);
            }
        }
        
        sql.append("  ").append(String.join(",\n  ", columnDefs));
        
        // 主键
        if (!pkColumns.isEmpty()) {
            sql.append(",\n  PRIMARY KEY (").append(String.join(", ", pkColumns)).append(")");
        }
        
        // 索引
        for (Map.Entry<String, List<IndexMeta>> entry : indexMap.entrySet()) {
            String indexName = entry.getKey();
            List<IndexMeta> indexMetas = entry.getValue();
            indexMetas.sort(Comparator.comparingInt(m -> m.order));
            
            boolean unique = indexMetas.get(0).unique;
            List<String> indexColumns = indexMetas.stream()
                    .map(m -> "`" + m.columnName + "`")
                    .toList();
            
            sql.append(",\n  ");
            if (unique) {
                sql.append("UNIQUE ");
            }
            sql.append("KEY `").append(indexName).append("` (")
                    .append(String.join(", ", indexColumns)).append(")");
        }
        
        sql.append("\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
        
        return sql.toString();
    }

    /**
     * 生成删表 SQL
     */
    public static String generateDropTable(Class<? extends BaseEntity<?>> entityClass) {
        return "DROP TABLE IF EXISTS `" + getTableName(entityClass) + "`;";
    }

    /**
     * 生成清空表 SQL
     */
    public static String generateTruncateTable(Class<? extends BaseEntity<?>> entityClass) {
        return "TRUNCATE TABLE `" + getTableName(entityClass) + "`;";
    }

    /**
     * 批量生成建表 SQL
     */
    @SafeVarargs
    public static String generateCreateTables(Class<? extends BaseEntity<?>>... entityClasses) {
        StringBuilder sql = new StringBuilder();
        for (Class<? extends BaseEntity<?>> clazz : entityClasses) {
            sql.append(generateCreateTable(clazz)).append("\n\n");
        }
        return sql.toString().trim();
    }

    // ==================== 私有方法 ====================

    private static String getTableName(Class<?> entityClass) {
        Table tableAnn = entityClass.getAnnotation(Table.class);
        if (tableAnn != null) {
            return tableAnn.value();
        }
        return StringUtils.camelToSnake(entityClass.getSimpleName());
    }

    private static List<FieldMeta> parseFields(Class<?> clazz) {
        List<FieldMeta> fields = new ArrayList<>();
        parseFieldsRecursive(clazz, fields);
        return fields;
    }

    private static void parseFieldsRecursive(Class<?> clazz, List<FieldMeta> fields) {
        if (clazz == null || clazz == BaseEntity.class || clazz == Object.class) {
            return;
        }
        // 先处理父类
        parseFieldsRecursive(clazz.getSuperclass(), fields);

        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isTransient(field.getModifiers()) ||
                Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            FieldMeta meta = new FieldMeta();
            meta.fieldName = field.getName();
            meta.fieldType = field.getType();

            // @Column
            Column columnAnn = field.getAnnotation(Column.class);
            if (columnAnn != null) {
                meta.columnName = columnAnn.value().isEmpty() ? StringUtils.camelToSnake(field.getName()) : columnAnn.value();
                meta.nullable = columnAnn.nullable();
                meta.columnType = columnAnn.columnType();
                meta.defaultValue = columnAnn.defaultValue();
            } else {
                meta.columnName = StringUtils.camelToSnake(field.getName());
                meta.nullable = true;
            }

            // @PrimaryKey
            PrimaryKey pkAnn = field.getAnnotation(PrimaryKey.class);
            if (pkAnn != null) {
                meta.primaryKey = true;
                meta.autoIncrement = pkAnn.autoIncrement();
                meta.nullable = false;
            }

            // @Index / @Indexes
            Indexes indexesAnn = field.getAnnotation(Indexes.class);
            if (indexesAnn != null) {
                for (Index idx : indexesAnn.value()) {
                    meta.indexes.add(createIndexMeta(idx, meta.columnName));
                }
            }
            Index indexAnn = field.getAnnotation(Index.class);
            if (indexAnn != null) {
                meta.indexes.add(createIndexMeta(indexAnn, meta.columnName));
            }

            fields.add(meta);
        }
    }

    private static IndexMeta createIndexMeta(Index idx, String columnName) {
        IndexMeta meta = new IndexMeta();
        meta.name = idx.name().isEmpty() ? "idx_" + columnName : idx.name();
        meta.unique = idx.unique();
        meta.order = idx.order();
        meta.columnName = columnName;
        return meta;
    }

    private static String buildColumnDefinition(FieldMeta field) {
        StringBuilder def = new StringBuilder();
        def.append("`").append(field.columnName).append("` ");

        // 列类型
        String sqlType = field.columnType.isEmpty() ? mapJavaTypeToSql(field.fieldType) : field.columnType;
        def.append(sqlType);

        // NOT NULL
        if (!field.nullable) {
            def.append(" NOT NULL");
        }

        // AUTO_INCREMENT
        if (field.autoIncrement) {
            def.append(" AUTO_INCREMENT");
        }

        // DEFAULT
        if (!field.defaultValue.isEmpty()) {
            def.append(" DEFAULT ").append(field.defaultValue);
        }

        return def.toString();
    }

    private static String mapJavaTypeToSql(Class<?> javaType) {
        if (javaType == long.class || javaType == Long.class) {
            return "BIGINT";
        } else if (javaType == int.class || javaType == Integer.class) {
            return "INT";
        } else if (javaType == short.class || javaType == Short.class) {
            return "SMALLINT";
        } else if (javaType == byte.class || javaType == Byte.class) {
            return "TINYINT";
        } else if (javaType == boolean.class || javaType == Boolean.class) {
            return "TINYINT(1)";
        } else if (javaType == float.class || javaType == Float.class) {
            return "FLOAT";
        } else if (javaType == double.class || javaType == Double.class) {
            return "DOUBLE";
        } else if (javaType == String.class) {
            return "VARCHAR(255)";
        } else if (javaType == java.util.Date.class || javaType == java.sql.Timestamp.class) {
            return "DATETIME";
        } else if (javaType == java.sql.Date.class) {
            return "DATE";
        } else if (javaType == java.sql.Time.class) {
            return "TIME";
        } else if (javaType == byte[].class) {
            return "BLOB";
        } else if (javaType == java.math.BigDecimal.class) {
            return "DECIMAL(19,4)";
        } else {
            // 其他类型默认用 TEXT（可存 JSON 等）
            return "TEXT";
        }
    }

}
