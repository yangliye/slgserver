package com.muyi.db.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.muyi.db.core.BaseEntity;
import com.muyi.db.core.EntityState;
import com.muyi.db.exception.DbException;

/**
 * SQL 执行器
 * <p>
 * 封装 JDBC 操作，提供同步和异步执行能力
 * </p>
 */
public class SqlExecutor {

    private static final Logger logger = LoggerFactory.getLogger(SqlExecutor.class);

    private final DataSource dataSource;

    public SqlExecutor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ==================== 单条操作 ====================

    /**
     * 插入实体
     */
    public <T extends BaseEntity<T>> boolean insert(T entity) {
        String sql = SqlBuilder.buildInsert(entity);
        Object[] values = SqlBuilder.getInsertValues(entity);
        
        try {
            int rows = executeUpdate(sql, values);
            if (rows > 0) {
                entity.setState(EntityState.PERSISTENT);
                entity.clearChanges();
                entity.syncVersion();
                return true;
            }
        } catch (SQLException e) {
            logger.error("Insert failed: {}", sql, e);
        }
        return false;
    }

    /**
     * 插入实体（自增主键回填）
     */
    public <T extends BaseEntity<T>> boolean insertWithGeneratedKey(T entity, String keyField) {
        String sql = SqlBuilder.buildInsertWithoutAutoIncrement(entity);
        Object[] values = SqlBuilder.getInsertValuesWithoutAutoIncrement(entity);
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            setParameters(ps, values);
            int rows = ps.executeUpdate();
            
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        long generatedKey = rs.getLong(1);
                        entity.getMetadata().setFieldValue(entity, keyField, generatedKey);
                    }
                }
                entity.setState(EntityState.PERSISTENT);
                entity.clearChanges();
                entity.syncVersion();
                return true;
            }
        } catch (SQLException e) {
            logger.error("Insert with generated key failed: {}", sql, e);
        }
        return false;
    }

    /**
     * 更新实体（全量更新）
     */
    public <T extends BaseEntity<T>> boolean update(T entity) {
        String sql = SqlBuilder.buildUpdate(entity);
        Object[] values = SqlBuilder.getUpdateValues(entity);
        
        try {
            int rows = executeUpdate(sql, values);
            if (rows > 0) {
                entity.clearChanges();
                entity.syncVersion();
                return true;
            }
        } catch (SQLException e) {
            logger.error("Update failed: {}", sql, e);
        }
        return false;
    }

    /**
     * 更新实体（只更新变更字段）
     */
    public <T extends BaseEntity<T>> boolean updatePartial(T entity) {
        if (!entity.hasChanges()) {
            return true;
        }
        
        String sql = SqlBuilder.buildPartialUpdate(entity);
        if (sql == null) {
            return true;
        }
        
        Object[] values = SqlBuilder.getPartialUpdateValues(entity);
        
        try {
            int rows = executeUpdate(sql, values);
            if (rows > 0) {
                entity.clearChanges();
                entity.syncVersion();
                return true;
            }
        } catch (SQLException e) {
            logger.error("Partial update failed: {}", sql, e);
        }
        return false;
    }

    /**
     * 插入或更新（UPSERT）
     */
    public <T extends BaseEntity<T>> boolean upsert(T entity) {
        String sql = SqlBuilder.buildUpsert(entity);
        Object[] values = SqlBuilder.getInsertValues(entity);
        
        try {
            int rows = executeUpdate(sql, values);
            if (rows > 0) {
                entity.setState(EntityState.PERSISTENT);
                entity.clearChanges();
                entity.syncVersion();
                return true;
            }
        } catch (SQLException e) {
            logger.error("Upsert failed: {}", sql, e);
        }
        return false;
    }

    /**
     * 删除实体
     */
    public <T extends BaseEntity<T>> boolean delete(T entity) {
        String sql = SqlBuilder.buildDelete(entity);
        Object[] values = SqlBuilder.getDeleteValues(entity);
        
        try {
            int rows = executeUpdate(sql, values);
            if (rows > 0) {
                entity.setState(EntityState.DELETED);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Delete failed: {}", sql, e);
        }
        return false;
    }

    // ==================== 查询操作 ====================

    /**
     * 按主键查询
     */
    public <T extends BaseEntity<T>> T selectByPrimaryKey(T template, Object... pkValues) {
        String sql = SqlBuilder.buildSelectByPrimaryKey(template);
        
        try {
            List<T> list = executeQuery(sql, template, pkValues);
            return list.isEmpty() ? null : list.get(0);
        } catch (SQLException e) {
            logger.error("Select by primary key failed: {}", sql, e);
            return null;
        }
    }

    /**
     * 按条件查询列表
     */
    public <T extends BaseEntity<T>> List<T> selectByCondition(T template, Map<String, Object> conditions) {
        String sql = SqlBuilder.buildSelectByCondition(template, conditions);
        Object[] values = conditions != null ? conditions.values().toArray() : new Object[0];
        
        try {
            return executeQuery(sql, template, values);
        } catch (SQLException e) {
            logger.error("Select by condition failed: {}", sql, e);
            return Collections.emptyList();
        }
    }

    /**
     * 执行自定义查询 SQL
     */
    public <T extends BaseEntity<T>> List<T> selectBySql(T template, String sql, Object... params) {
        try {
            return executeQuery(sql, template, params);
        } catch (SQLException e) {
            logger.error("Select by sql failed: {}", sql, e);
            return Collections.emptyList();
        }
    }

    /**
     * 执行自定义查询 SQL，返回 Map 列表
     */
    public List<Map<String, Object>> selectMapBySql(String sql, Object... params) {
        try {
            return executeQueryMap(sql, params);
        } catch (SQLException e) {
            logger.error("Select map by sql failed: {}", sql, e);
            return Collections.emptyList();
        }
    }

    /**
     * 查询单个值（用于 COUNT、SUM、MAX 等聚合查询）
     * <p>
     * 示例：
     * <pre>{@code
     * long count = sqlExecutor.selectOne("SELECT COUNT(*) FROM player WHERE level > ?", Long.class, 0L, 10);
     * String name = sqlExecutor.selectOne("SELECT name FROM player WHERE uid = ?", String.class, null, 10001L);
     * }</pre>
     *
     * @param sql SQL 语句
     * @param resultType 结果类型
     * @param defaultValue 默认值（查询无结果或结果为 null 时返回）
     * @param params SQL 参数
     * @return 查询结果，如果没有结果返回默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T selectOne(String sql, Class<T> resultType, T defaultValue, Object... params) {
        List<Map<String, Object>> result = selectMapBySql(sql, params);
        if (result.isEmpty()) {
            return defaultValue;
        }
        
        Map<String, Object> row = result.get(0);
        
        // 如果目标类型是 BaseEntity，从 Map 构建实体
        if (BaseEntity.class.isAssignableFrom(resultType)) {
            try {
                BaseEntity<?> entity = (BaseEntity<?>) resultType.getDeclaredConstructor().newInstance();
                entity.fromMap(row);
                return (T) entity;
            } catch (Exception e) {
                logger.error("Failed to create entity instance: {}", resultType.getName(), e);
                return defaultValue;
            }
        }
        
        // 普通类型：取第一列的值
        Object value = row.values().iterator().next();
        if (value == null) {
            return defaultValue;
        }
        return (T) convertToType(value, resultType);
    }

    private Object convertToType(Object value, Class<?> targetType) {
        if (value == null || targetType == null) {
            return value;
        }
        if (targetType.isInstance(value)) {
            return value;
        }
        
        // 数值类型转换
        if (value instanceof Number num) {
            if (targetType == Long.class || targetType == long.class) {
                return num.longValue();
            } else if (targetType == Integer.class || targetType == int.class) {
                return num.intValue();
            } else if (targetType == Double.class || targetType == double.class) {
                return num.doubleValue();
            } else if (targetType == Float.class || targetType == float.class) {
                return num.floatValue();
            } else if (targetType == Short.class || targetType == short.class) {
                return num.shortValue();
            } else if (targetType == Byte.class || targetType == byte.class) {
                return num.byteValue();
            }
        }
        
        // 字符串转换
        if (targetType == String.class) {
            return value.toString();
        }
        
        return value;
    }

    // ==================== 批量操作 ====================

    /**
     * 批量插入
     */
    public <T extends BaseEntity<T>> int[] batchInsert(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return new int[0];
        }
        
        T first = entities.get(0);
        String sql = SqlBuilder.buildInsert(first);
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            
            try {
                for (T entity : entities) {
                    Object[] values = SqlBuilder.getInsertValues(entity);
                    setParameters(ps, values);
                    ps.addBatch();
                }
                
                int[] results = ps.executeBatch();
                conn.commit();
                
                // 更新实体状态
                for (int i = 0; i < entities.size(); i++) {
                    if (results[i] > 0) {
                        T entity = entities.get(i);
                        entity.setState(EntityState.PERSISTENT);
                        entity.clearChanges();
                        entity.syncVersion();
                    }
                }
                
                return results;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            logger.error("Batch insert failed: {}", sql, e);
            throw new DbException(DbException.OperationType.BATCH_INSERT, 
                    "Batch insert failed: " + e.getMessage(), e);
        }
    }

    /**
     * 批量更新
     */
    public <T extends BaseEntity<T>> int[] batchUpdate(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return new int[0];
        }
        
        T first = entities.get(0);
        String sql = SqlBuilder.buildUpdate(first);
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            
            try {
                for (T entity : entities) {
                    Object[] values = SqlBuilder.getUpdateValues(entity);
                    setParameters(ps, values);
                    ps.addBatch();
                }
                
                int[] results = ps.executeBatch();
                conn.commit();
                
                // 更新实体状态
                for (int i = 0; i < entities.size(); i++) {
                    if (results[i] > 0) {
                        T entity = entities.get(i);
                        entity.clearChanges();
                        entity.syncVersion();
                    }
                }
                
                return results;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            logger.error("Batch update failed: {}", sql, e);
            throw new DbException(DbException.OperationType.BATCH_UPDATE, 
                    "Batch update failed: " + e.getMessage(), e);
        }
    }

    /**
     * 批量删除
     */
    public <T extends BaseEntity<T>> int[] batchDelete(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return new int[0];
        }
        
        T first = entities.get(0);
        String sql = SqlBuilder.buildDelete(first);
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            
            try {
                for (T entity : entities) {
                    Object[] values = SqlBuilder.getDeleteValues(entity);
                    setParameters(ps, values);
                    ps.addBatch();
                }
                
                int[] results = ps.executeBatch();
                conn.commit();
                
                // 更新实体状态
                for (int i = 0; i < entities.size(); i++) {
                    if (results[i] > 0) {
                        entities.get(i).setState(EntityState.DELETED);
                    }
                }
                
                return results;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            logger.error("Batch delete failed: {}", sql, e);
            throw new DbException(DbException.OperationType.BATCH_DELETE, 
                    "Batch delete failed: " + e.getMessage(), e);
        }
    }

    // ==================== 事务操作 ====================

    /**
     * 在事务中执行
     */
    public void executeInTransaction(Consumer<Connection> action) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                action.accept(conn);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            logger.error("Transaction failed", e);
            throw new RuntimeException(e);
        }
    }

    // ==================== 底层执行方法 ====================

    private int executeUpdate(String sql, Object[] params) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            setParameters(ps, params);
            
            if (logger.isDebugEnabled()) {
                logger.debug("Execute SQL: {} | Params: {}", sql, Arrays.toString(params));
            }
            
            return ps.executeUpdate();
        }
    }

    private <T extends BaseEntity<T>> List<T> executeQuery(String sql, T template, Object[] params) throws SQLException {
        List<T> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            setParameters(ps, params);
            
            if (logger.isDebugEnabled()) {
                logger.debug("Execute Query: {} | Params: {}", sql, Arrays.toString(params));
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(rsmd.getColumnLabel(i), rs.getObject(i));
                    }
                    
                    T entity = template.newInstance();
                    entity.fromMap(row);
                    results.add(entity);
                }
            }
        }
        
        return results;
    }

    private List<Map<String, Object>> executeQueryMap(String sql, Object[] params) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            setParameters(ps, params);
            
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(rsmd.getColumnLabel(i), rs.getObject(i));
                    }
                    results.add(row);
                }
            }
        }
        
        return results;
    }

    private void setParameters(PreparedStatement ps, Object[] params) throws SQLException {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
        }
    }

    /**
     * 执行原生 SQL 更新
     */
    public int executeSql(String sql, Object... params) {
        try {
            return executeUpdate(sql, params);
        } catch (SQLException e) {
            logger.error("Execute sql failed: {}", sql, e);
            return 0;
        }
    }
}
