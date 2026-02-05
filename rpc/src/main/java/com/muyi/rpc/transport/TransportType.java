package com.muyi.rpc.transport;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * 传输类型枚举
 * 根据平台自动选择最优实现（Netty 4.2+ 新 API）
 * 
 * - Linux: Epoll（边缘触发，高性能）
 * - macOS/BSD: KQueue
 * - Windows/其他: Java NIO
 *
 * @author muyi
 */
public enum TransportType {
    
    /** Linux Epoll - 高性能边缘触发 */
    EPOLL("Linux Epoll") {
        @Override
        public Class<? extends ServerChannel> serverChannelClass() {
            return EpollServerSocketChannel.class;
        }
        
        @Override
        public Class<? extends SocketChannel> socketChannelClass() {
            return EpollSocketChannel.class;
        }
        
        @Override
        public EventLoopGroup createEventLoopGroup(int threads) {
            return threads > 0 
                    ? new MultiThreadIoEventLoopGroup(threads, EpollIoHandler.newFactory())
                    : new MultiThreadIoEventLoopGroup(EpollIoHandler.newFactory());
        }
    },
    
    /** macOS/BSD KQueue */
    KQUEUE("macOS/BSD KQueue") {
        @Override
        public Class<? extends ServerChannel> serverChannelClass() {
            return KQueueServerSocketChannel.class;
        }
        
        @Override
        public Class<? extends SocketChannel> socketChannelClass() {
            return KQueueSocketChannel.class;
        }
        
        @Override
        public EventLoopGroup createEventLoopGroup(int threads) {
            return threads > 0 
                    ? new MultiThreadIoEventLoopGroup(threads, KQueueIoHandler.newFactory())
                    : new MultiThreadIoEventLoopGroup(KQueueIoHandler.newFactory());
        }
    },
    
    /** 通用 Java NIO（Windows 等） */
    NIO("Java NIO") {
        @Override
        public Class<? extends ServerChannel> serverChannelClass() {
            return NioServerSocketChannel.class;
        }
        
        @Override
        public Class<? extends SocketChannel> socketChannelClass() {
            return NioSocketChannel.class;
        }
        
        @Override
        public EventLoopGroup createEventLoopGroup(int threads) {
            return threads > 0 
                    ? new MultiThreadIoEventLoopGroup(threads, NioIoHandler.newFactory())
                    : new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        }
    };
    
    private final String description;
    
    TransportType(String description) {
        this.description = description;
    }
    
    public String description() {
        return description;
    }
    
    public abstract Class<? extends ServerChannel> serverChannelClass();
    
    public abstract Class<? extends SocketChannel> socketChannelClass();
    
    public abstract EventLoopGroup createEventLoopGroup(int threads);
    
    /**
     * 检测当前平台支持的最优传输类型
     */
    public static TransportType detect() {
        if (Epoll.isAvailable()) {
            return EPOLL;
        }
        if (KQueue.isAvailable()) {
            return KQUEUE;
        }
        return NIO;
    }
    
    /**
     * 是否是 Epoll
     */
    public boolean isEpoll() {
        return this == EPOLL;
    }
    
    /**
     * 是否是 KQueue
     */
    public boolean isKQueue() {
        return this == KQUEUE;
    }
    
    /**
     * 是否是原生实现（非 NIO）
     */
    public boolean isNative() {
        return this != NIO;
    }
}

