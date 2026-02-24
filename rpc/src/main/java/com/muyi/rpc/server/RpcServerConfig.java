package com.muyi.rpc.server;

/**
 * RPC 服务端配置
 * 每个 RpcServer 可独立配置参数
 *
 * @author muyi
 */
public class RpcServerConfig {

    /** SO_BACKLOG: 全连接队列大小 */
    private int backlog = 4096;

    /** 读空闲超时（秒），超时后关闭连接 */
    private int readerIdleTimeSeconds = 60;

    /** SO_SNDBUF: 发送缓冲区（字节） */
    private int sendBufferSize = 256 * 1024;

    /** SO_RCVBUF: 接收缓冲区（字节） */
    private int receiveBufferSize = 256 * 1024;

    /** 写缓冲区低水位线（字节） */
    private int writeLowWaterMark = 64 * 1024;

    /** 写缓冲区高水位线（字节） */
    private int writeHighWaterMark = 128 * 1024;

    /** TCP_NODELAY: 禁用 Nagle 算法 */
    private boolean tcpNoDelay = true;

    /** SO_KEEPALIVE: TCP 心跳保活 */
    private boolean keepAlive = true;

    /** SO_REUSEADDR: 允许重用 TIME_WAIT 地址 */
    private boolean reuseAddress = true;

    /** 优雅关闭等待时间（秒） */
    private int shutdownTimeoutSeconds = 15;

    // ==================== Builder 方法 ====================

    public RpcServerConfig backlog(int backlog) {
        this.backlog = backlog;
        return this;
    }

    public RpcServerConfig readerIdleTimeSeconds(int seconds) {
        this.readerIdleTimeSeconds = seconds;
        return this;
    }

    public RpcServerConfig sendBufferSize(int bytes) {
        this.sendBufferSize = bytes;
        return this;
    }

    public RpcServerConfig receiveBufferSize(int bytes) {
        this.receiveBufferSize = bytes;
        return this;
    }

    public RpcServerConfig writeLowWaterMark(int bytes) {
        this.writeLowWaterMark = bytes;
        return this;
    }

    public RpcServerConfig writeHighWaterMark(int bytes) {
        this.writeHighWaterMark = bytes;
        return this;
    }

    public RpcServerConfig tcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
        return this;
    }

    public RpcServerConfig keepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
        return this;
    }

    public RpcServerConfig reuseAddress(boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
        return this;
    }

    public RpcServerConfig shutdownTimeoutSeconds(int seconds) {
        this.shutdownTimeoutSeconds = seconds;
        return this;
    }

    // ==================== Getter ====================

    public int getBacklog() {
        return backlog;
    }

    public int getReaderIdleTimeSeconds() {
        return readerIdleTimeSeconds;
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

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public boolean isReuseAddress() {
        return reuseAddress;
    }

    public int getShutdownTimeoutSeconds() {
        return shutdownTimeoutSeconds;
    }
}
