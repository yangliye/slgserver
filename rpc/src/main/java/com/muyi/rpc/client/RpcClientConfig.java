package com.muyi.rpc.client;

/**
 * RPC 客户端配置
 * 全局共享，所有 RpcClient 使用同一份配置
 *
 * @author muyi
 */
public class RpcClientConfig {

    /** 连接超时（毫秒） */
    private int connectTimeout = 5_000;

    /** 默认请求超时（毫秒） */
    private long requestTimeout = 10_000;

    /** 心跳间隔（秒） */
    private int heartbeatInterval = 30;

    /** 心跳最大失败次数，超过后断开连接 */
    private int heartbeatMaxFailCount = 3;

    /** 每个地址的最大连接数 */
    private int maxConnectionsPerAddress = 10;

    /** 连接池初始连接数 */
    private int poolInitialConnections = 3;

    /** 默认重试次数 */
    private int retries = 1;

    /** SO_SNDBUF 发送缓冲区（字节） */
    private int sendBufferSize = 256 * 1024;

    /** SO_RCVBUF 接收缓冲区（字节） */
    private int receiveBufferSize = 256 * 1024;

    /** 写缓冲区低水位线（字节） */
    private int writeLowWaterMark = 64 * 1024;

    /** 写缓冲区高水位线（字节） */
    private int writeHighWaterMark = 128 * 1024;

    /** 时间轮 tick 间隔（毫秒） */
    private int wheelTimerTickMs = 100;

    /** 时间轮槽数 */
    private int wheelTimerTicks = 512;

    /** 优雅关闭等待时间（秒） */
    private int shutdownTimeoutSeconds = 15;

    /** 重试初始延迟（毫秒），用于指数退避 */
    private long retryInitialDelayMs = 100;

    /** 重试最大延迟（毫秒），用于指数退避 */
    private long retryMaxDelayMs = 2000;

    // ==================== Builder 方法 ====================

    public RpcClientConfig connectTimeout(int ms) {
        this.connectTimeout = ms;
        return this;
    }

    public RpcClientConfig requestTimeout(long ms) {
        this.requestTimeout = ms;
        return this;
    }

    public RpcClientConfig heartbeatInterval(int seconds) {
        this.heartbeatInterval = seconds;
        return this;
    }

    public RpcClientConfig heartbeatMaxFailCount(int count) {
        this.heartbeatMaxFailCount = count;
        return this;
    }

    public RpcClientConfig maxConnectionsPerAddress(int max) {
        this.maxConnectionsPerAddress = max;
        return this;
    }

    public RpcClientConfig poolInitialConnections(int count) {
        this.poolInitialConnections = count;
        return this;
    }

    public RpcClientConfig retries(int retries) {
        this.retries = retries;
        return this;
    }

    public RpcClientConfig sendBufferSize(int bytes) {
        this.sendBufferSize = bytes;
        return this;
    }

    public RpcClientConfig receiveBufferSize(int bytes) {
        this.receiveBufferSize = bytes;
        return this;
    }

    public RpcClientConfig writeLowWaterMark(int bytes) {
        this.writeLowWaterMark = bytes;
        return this;
    }

    public RpcClientConfig writeHighWaterMark(int bytes) {
        this.writeHighWaterMark = bytes;
        return this;
    }

    public RpcClientConfig wheelTimerTickMs(int ms) {
        this.wheelTimerTickMs = ms;
        return this;
    }

    public RpcClientConfig wheelTimerTicks(int ticks) {
        this.wheelTimerTicks = ticks;
        return this;
    }

    public RpcClientConfig shutdownTimeoutSeconds(int seconds) {
        this.shutdownTimeoutSeconds = seconds;
        return this;
    }

    public RpcClientConfig retryInitialDelayMs(long ms) {
        this.retryInitialDelayMs = ms;
        return this;
    }

    public RpcClientConfig retryMaxDelayMs(long ms) {
        this.retryMaxDelayMs = ms;
        return this;
    }

    // ==================== Getter ====================

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public long getRequestTimeout() {
        return requestTimeout;
    }

    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public int getHeartbeatMaxFailCount() {
        return heartbeatMaxFailCount;
    }

    public int getMaxConnectionsPerAddress() {
        return maxConnectionsPerAddress;
    }

    public int getPoolInitialConnections() {
        return poolInitialConnections;
    }

    public int getRetries() {
        return retries;
    }

    public int getSendBufferSize() {
        return sendBufferSize;
    }

    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    public int getWriteLowWaterMark() {
        return writeLowWaterMark;
    }

    public int getWriteHighWaterMark() {
        return writeHighWaterMark;
    }

    public int getWheelTimerTickMs() {
        return wheelTimerTickMs;
    }

    public int getWheelTimerTicks() {
        return wheelTimerTicks;
    }

    public int getShutdownTimeoutSeconds() {
        return shutdownTimeoutSeconds;
    }

    public long getRetryInitialDelayMs() {
        return retryInitialDelayMs;
    }

    public long getRetryMaxDelayMs() {
        return retryMaxDelayMs;
    }
}
