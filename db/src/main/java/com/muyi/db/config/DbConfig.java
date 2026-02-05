package com.muyi.db.config;

import java.util.Objects;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * 数据库配置
 */
public class DbConfig {

    private String jdbcUrl;
    private String username;
    private String password;
    private String driverClassName = "com.mysql.cj.jdbc.Driver";

    // 连接池配置
    private int maximumPoolSize = 10;
    private int minimumIdle = 5;
    private long connectionTimeout = 30000;
    private long idleTimeout = 600000;
    private long maxLifetime = 1800000;

    // 异步落地配置（基于压测的最优配置）
    // 测试结论：8线程/10ms/500批量 吞吐量最高 (80万 ops/s)
    // 但考虑到通用性和资源占用，使用较保守的配置
    private int landThreads = 4;
    private long landIntervalMs = 25;      // 原 50ms，优化后 25ms
    private int landBatchSize = 400;       // 原 200，优化后 400
    private int landMaxRetries = 3;

    // 日志配置
    private boolean logSql = false;

    public DbConfig() {
    }

    public DbConfig(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    /**
     * 创建 HikariCP 数据源
     * 
     * @throws IllegalArgumentException 如果必要参数为空
     */
    public DataSource createDataSource() {
        // 参数校验
        Objects.requireNonNull(jdbcUrl, "jdbcUrl cannot be null");
        Objects.requireNonNull(username, "username cannot be null");
        Objects.requireNonNull(password, "password cannot be null");
        
        if (jdbcUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("jdbcUrl cannot be empty");
        }
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);

        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);

        // MySQL 优化配置
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");

        return new HikariDataSource(config);
    }

    // ==================== Builder 方法 ====================

    public DbConfig jdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
        return this;
    }

    public DbConfig username(String username) {
        this.username = username;
        return this;
    }

    public DbConfig password(String password) {
        this.password = password;
        return this;
    }

    public DbConfig driverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
        return this;
    }

    public DbConfig maximumPoolSize(int maximumPoolSize) {
        if (maximumPoolSize <= 0) {
            throw new IllegalArgumentException("maximumPoolSize must be positive, got: " + maximumPoolSize);
        }
        this.maximumPoolSize = maximumPoolSize;
        return this;
    }

    public DbConfig minimumIdle(int minimumIdle) {
        if (minimumIdle < 0) {
            throw new IllegalArgumentException("minimumIdle cannot be negative, got: " + minimumIdle);
        }
        this.minimumIdle = minimumIdle;
        return this;
    }

    public DbConfig connectionTimeout(long connectionTimeout) {
        if (connectionTimeout <= 0) {
            throw new IllegalArgumentException("connectionTimeout must be positive, got: " + connectionTimeout);
        }
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public DbConfig idleTimeout(long idleTimeout) {
        if (idleTimeout < 0) {
            throw new IllegalArgumentException("idleTimeout cannot be negative, got: " + idleTimeout);
        }
        this.idleTimeout = idleTimeout;
        return this;
    }

    public DbConfig maxLifetime(long maxLifetime) {
        if (maxLifetime <= 0) {
            throw new IllegalArgumentException("maxLifetime must be positive, got: " + maxLifetime);
        }
        this.maxLifetime = maxLifetime;
        return this;
    }

    public DbConfig landThreads(int landThreads) {
        if (landThreads <= 0) {
            throw new IllegalArgumentException("landThreads must be positive, got: " + landThreads);
        }
        this.landThreads = landThreads;
        return this;
    }

    public DbConfig landIntervalMs(long landIntervalMs) {
        if (landIntervalMs <= 0) {
            throw new IllegalArgumentException("landIntervalMs must be positive, got: " + landIntervalMs);
        }
        this.landIntervalMs = landIntervalMs;
        return this;
    }

    public DbConfig landBatchSize(int landBatchSize) {
        if (landBatchSize <= 0) {
            throw new IllegalArgumentException("landBatchSize must be positive, got: " + landBatchSize);
        }
        this.landBatchSize = landBatchSize;
        return this;
    }

    public DbConfig landMaxRetries(int landMaxRetries) {
        if (landMaxRetries < 0) {
            throw new IllegalArgumentException("landMaxRetries cannot be negative, got: " + landMaxRetries);
        }
        this.landMaxRetries = landMaxRetries;
        return this;
    }

    public DbConfig logSql(boolean logSql) {
        this.logSql = logSql;
        return this;
    }

    // ==================== Getter ====================

    public String getJdbcUrl() { return jdbcUrl; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getDriverClassName() { return driverClassName; }
    public int getMaximumPoolSize() { return maximumPoolSize; }
    public int getMinimumIdle() { return minimumIdle; }
    public long getConnectionTimeout() { return connectionTimeout; }
    public long getIdleTimeout() { return idleTimeout; }
    public long getMaxLifetime() { return maxLifetime; }
    public int getLandThreads() { return landThreads; }
    public long getLandIntervalMs() { return landIntervalMs; }
    public int getLandBatchSize() { return landBatchSize; }
    public int getLandMaxRetries() { return landMaxRetries; }
    public boolean isLogSql() { return logSql; }
}
