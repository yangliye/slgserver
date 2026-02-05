package com.muyi.db.exception;

/**
 * 数据库操作异常
 * <p>
 * 封装数据库操作中的所有异常，便于调用方统一处理
 */
public class DbException extends RuntimeException {

    /**
     * 操作类型
     */
    private final OperationType operationType;

    public DbException(String message) {
        super(message);
        this.operationType = OperationType.UNKNOWN;
    }

    public DbException(String message, Throwable cause) {
        super(message, cause);
        this.operationType = OperationType.UNKNOWN;
    }

    public DbException(OperationType operationType, String message) {
        super(message);
        this.operationType = operationType;
    }

    public DbException(OperationType operationType, String message, Throwable cause) {
        super(message, cause);
        this.operationType = operationType;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    /**
     * 操作类型枚举
     */
    public enum OperationType {
        INSERT,
        UPDATE,
        DELETE,
        SELECT,
        BATCH_INSERT,
        BATCH_UPDATE,
        BATCH_DELETE,
        TRANSACTION,
        UNKNOWN
    }
}
