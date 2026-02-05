package com.muyi.db.async;

/**
 * 异步落地配置
 */
public class AsyncLandConfig {
    /**
     * 落地线程数
     */
    int landThreads = 4;

    /**
     * 落地间隔（毫秒）
     * <p>
     * 基于压测结论：25ms 比 50ms 性能提升约 18%
     */
    long landIntervalMs = 25;

    /**
     * 批量大小
     * <p>
     * 基于压测结论：400 比 200 性能提升约 18%
     */
    int batchSize = 400;

    /**
     * 最大重试次数
     */
    int maxRetries = 3;

    public AsyncLandConfig landThreads(int threads) {
        if (threads <= 0) {
            throw new IllegalArgumentException("landThreads must be positive, got: " + threads);
        }
        this.landThreads = threads;
        return this;
    }

    public AsyncLandConfig landIntervalMs(long intervalMs) {
        if (intervalMs <= 0) {
            throw new IllegalArgumentException("landIntervalMs must be positive, got: " + intervalMs);
        }
        this.landIntervalMs = intervalMs;
        return this;
    }

    public AsyncLandConfig batchSize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("batchSize must be positive, got: " + size);
        }
        this.batchSize = size;
        return this;
    }

    public AsyncLandConfig maxRetries(int retries) {
        if (retries < 0) {
            throw new IllegalArgumentException("maxRetries cannot be negative, got: " + retries);
        }
        this.maxRetries = retries;
        return this;
    }
    
    // Getters
    public int getLandThreads() {
        return landThreads;
    }
    
    public long getLandIntervalMs() {
        return landIntervalMs;
    }
    
    public int getBatchSize() {
        return batchSize;
    }
    
    public int getMaxRetries() {
        return maxRetries;
    }
}
