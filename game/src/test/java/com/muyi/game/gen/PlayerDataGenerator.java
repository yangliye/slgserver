package com.muyi.game.gen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 从 MySQL 数据库表自动生成 Entity + Manager + Service Java 类
 * <p>
 * 使用方式：修改 {@link #main(String[])} 中的配置后直接运行
 * <p>
 * 生成规则：
 * <ul>
 *   <li>Entity: {@code @Table} + {@code @Column} + getter/setter，setter 自动调用 markChanged</li>
 *   <li>Manager: {@code @PlayerData} + 继承 {@code AbstractPlayerManager}，自动检测业务 key</li>
 *   <li>Service: 骨架类，已有文件不覆盖</li>
 * </ul>
 */
public class PlayerDataGenerator {

    // ==================== 配置区 ====================

    private static final String JDBC_URL = "jdbc:mysql://127.0.0.1:3306/slg_game_1?useSSL=false&characterEncoding=utf8";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASSWORD = "123456";

    private static final String JAVA_SRC_ROOT = "game/src/main/java";

    private static final String ENTITY_PKG = "com.muyi.game.entity";
    private static final String MANAGER_PKG = "com.muyi.game.manager";
    private static final String SERVICE_PKG = "com.muyi.game.service";

    private static final int ORDER_START = 10;
    private static final int ORDER_STEP = 10;

    // ==================== 入口 ====================

    /**
     * 修改 tables 列表后直接运行
     */
    public static void main(String[] args) throws Exception {
        List<String> tables = List.of(
                "t_hero",
                "t_building"
                // 添加更多表名...
        );

        Path projectRoot = resolveProjectRoot();
        Path srcRoot = projectRoot.resolve(JAVA_SRC_ROOT);

        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)) {
            int order = ORDER_START;
            for (String table : tables) {
                List<ColumnInfo> columns = describeTable(conn, table);
                if (columns.isEmpty()) {
                    System.err.println("[WARN] Table not found or empty: " + table);
                    continue;
                }

                String bizName = tableToBizName(table);
                String keyColumn = detectKeyColumn(columns);

                generateEntity(srcRoot, table, bizName, columns);
                generateManager(srcRoot, bizName, columns, keyColumn, order);
                generateService(srcRoot, bizName);

                System.out.printf("Generated: %sEntity, %sManager, %sService (table=%s, key=%s, order=%d)%n",
                        bizName, bizName, bizName, table, keyColumn, order);
                order += ORDER_STEP;
            }
        }

        System.out.println("Done!");
    }

    // ==================== 表结构读取 ====================

    static List<ColumnInfo> describeTable(Connection conn, String table) throws SQLException {
        List<ColumnInfo> columns = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW FULL COLUMNS FROM `" + table + "`")) {
            while (rs.next()) {
                ColumnInfo col = new ColumnInfo();
                col.name = rs.getString("Field");
                col.type = rs.getString("Type");
                col.nullable = "YES".equals(rs.getString("Null"));
                col.key = rs.getString("Key");
                col.extra = rs.getString("Extra");
                col.comment = rs.getString("Comment");
                columns.add(col);
            }
        }
        return columns;
    }

    // ==================== Entity 生成 ====================

    static void generateEntity(Path srcRoot, String table, String bizName, List<ColumnInfo> columns) throws IOException {
        String className = bizName + "Entity";
        StringBuilder sb = new StringBuilder();

        sb.append("package ").append(ENTITY_PKG).append(";\n\n");
        sb.append("import com.muyi.db.annotation.Column;\n");
        sb.append("import com.muyi.db.annotation.PrimaryKey;\n");
        sb.append("import com.muyi.db.annotation.Table;\n");
        sb.append("import com.muyi.db.core.BaseEntity;\n\n");

        sb.append("@Table(\"").append(table).append("\")\n");
        sb.append("public class ").append(className).append(" extends BaseEntity<").append(className).append("> {\n\n");

        // fields
        for (ColumnInfo col : columns) {
            String javaType = sqlToJavaType(col.type);
            String javaField = snakeToCamel(col.name, false);

            if (col.comment != null && !col.comment.isEmpty()) {
                sb.append("    /** ").append(col.comment).append(" */\n");
            }
            if (col.isPrimaryKey()) {
                sb.append("    @PrimaryKey(autoIncrement = ").append(col.isAutoIncrement()).append(")\n");
            }
            if (!col.name.equals(javaField)) {
                sb.append("    @Column(\"").append(col.name).append("\")\n");
            } else {
                sb.append("    @Column\n");
            }
            sb.append("    private ").append(javaType).append(" ").append(javaField).append(";\n\n");
        }

        // getter / setter
        for (ColumnInfo col : columns) {
            String javaType = sqlToJavaType(col.type);
            String javaField = snakeToCamel(col.name, false);
            String upperField = snakeToCamel(col.name, true);

            sb.append("    public ").append(javaType).append(" get").append(upperField).append("() {\n");
            sb.append("        return ").append(javaField).append(";\n");
            sb.append("    }\n\n");

            sb.append("    public void set").append(upperField).append("(").append(javaType).append(" ").append(javaField).append(") {\n");
            sb.append("        this.").append(javaField).append(" = ").append(javaField).append(";\n");
            if (!col.isPrimaryKey() && !"uid".equals(col.name)) {
                sb.append("        markChanged(\"").append(col.name).append("\");\n");
            }
            sb.append("    }\n\n");
        }

        sb.append("}\n");

        writeJavaFile(srcRoot, ENTITY_PKG, className, sb.toString());
    }

    // ==================== Manager 生成 ====================

    static void generateManager(Path srcRoot, String bizName, List<ColumnInfo> columns, String keyCol, int order)
            throws IOException {
        String className = bizName + "Manager";
        String entityClass = bizName + "Entity";
        String keyField = snakeToCamel(keyCol, false);
        String keyGetter = "get" + snakeToCamel(keyCol, true);

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(MANAGER_PKG).append(";\n\n");
        sb.append("import com.muyi.game.data.AbstractPlayerManager;\n");
        sb.append("import com.muyi.game.data.PlayerData;\n");
        sb.append("import ").append(ENTITY_PKG).append(".").append(entityClass).append(";\n\n");
        sb.append("import java.util.Collection;\n\n");

        sb.append("@PlayerData(order = ").append(order).append(")\n");
        sb.append("public class ").append(className).append(" extends AbstractPlayerManager<").append(entityClass).append("> {\n\n");

        sb.append("    @Override\n");
        sb.append("    protected Class<").append(entityClass).append("> entityClass() {\n");
        sb.append("        return ").append(entityClass).append(".class;\n");
        sb.append("    }\n\n");

        sb.append("    @Override\n");
        sb.append("    protected int keyOf(").append(entityClass).append(" entity) {\n");
        sb.append("        return entity.").append(keyGetter).append("();\n");
        sb.append("    }\n\n");

        sb.append("    public ").append(entityClass).append(" getBy").append(snakeToCamel(keyCol, true));
        sb.append("(int ").append(keyField).append(") {\n");
        sb.append("        return get(").append(keyField).append(");\n");
        sb.append("    }\n\n");

        sb.append("    public Collection<").append(entityClass).append("> getAll() {\n");
        sb.append("        return super.getAll();\n");
        sb.append("    }\n\n");

        sb.append("}\n");

        writeJavaFile(srcRoot, MANAGER_PKG, className, sb.toString());
    }

    // ==================== Service 生成 ====================

    static void generateService(Path srcRoot, String bizName) throws IOException {
        String className = bizName + "Service";
        Path file = resolveJavaFile(srcRoot, SERVICE_PKG, className);
        if (Files.exists(file)) {
            System.out.println("  [skip] " + className + ".java already exists");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(SERVICE_PKG).append(";\n\n");
        sb.append("import org.slf4j.Logger;\n");
        sb.append("import org.slf4j.LoggerFactory;\n\n");

        sb.append("public class ").append(className).append(" {\n\n");
        sb.append("    private static final Logger log = LoggerFactory.getLogger(").append(className).append(".class);\n\n");
        sb.append("    // TODO: add global service methods\n");
        sb.append("}\n");

        writeJavaFile(srcRoot, SERVICE_PKG, className, sb.toString());
    }

    // ==================== 工具方法 ====================

    static String detectKeyColumn(List<ColumnInfo> columns) {
        for (ColumnInfo col : columns) {
            if (!"id".equals(col.name) && !"uid".equals(col.name) && col.name.endsWith("_id")) {
                return col.name;
            }
        }
        for (ColumnInfo col : columns) {
            if (col.isPrimaryKey()) {
                return col.name;
            }
        }
        return "id";
    }

    static String tableToBizName(String table) {
        String name = table;
        if (name.startsWith("t_")) {
            name = name.substring(2);
        } else if (name.startsWith("tbl_")) {
            name = name.substring(4);
        }
        return snakeToCamel(name, true);
    }

    static String snakeToCamel(String s, boolean upperFirst) {
        String[] parts = s.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;
            if (i == 0 && !upperFirst) {
                sb.append(part);
            } else {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }

    static String sqlToJavaType(String sqlType) {
        String lower = sqlType.toLowerCase();
        if (lower.startsWith("bigint")) return "long";
        if (lower.startsWith("int") || lower.startsWith("mediumint")) return "int";
        if (lower.startsWith("smallint")) return "short";
        if (lower.startsWith("tinyint")) {
            return lower.contains("(1)") ? "boolean" : "int";
        }
        if (lower.startsWith("float")) return "float";
        if (lower.startsWith("double") || lower.startsWith("decimal")) return "double";
        if (lower.startsWith("varchar") || lower.startsWith("text") || lower.startsWith("char")
                || lower.startsWith("longtext") || lower.startsWith("mediumtext")) return "String";
        if (lower.startsWith("datetime") || lower.startsWith("timestamp")) return "long";
        if (lower.startsWith("blob") || lower.startsWith("longblob")) return "byte[]";
        return "String";
    }

    static Path resolveProjectRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (Files.exists(cwd.resolve("settings.gradle")) || Files.exists(cwd.resolve("build.gradle"))) {
            return cwd;
        }
        Path parent = cwd.getParent();
        if (parent != null && Files.exists(parent.resolve("settings.gradle"))) {
            return parent;
        }
        return cwd;
    }

    static Path resolveJavaFile(Path srcRoot, String pkg, String className) {
        String pkgPath = pkg.replace('.', '/');
        return srcRoot.resolve(pkgPath).resolve(className + ".java");
    }

    static void writeJavaFile(Path srcRoot, String pkg, String className, String content) throws IOException {
        Path file = resolveJavaFile(srcRoot, pkg, className);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    // ==================== 数据类 ====================

    static class ColumnInfo {
        String name;
        String type;
        boolean nullable;
        String key;
        String extra;
        String comment;

        boolean isPrimaryKey() {
            return "PRI".equals(key);
        }

        boolean isAutoIncrement() {
            return extra != null && extra.contains("auto_increment");
        }
    }
}
