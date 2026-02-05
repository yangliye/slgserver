package com.muyi.rpc.protocol;

/**
 * RPC消息类型
 *
 * @author muyi
 */
public class MessageType {
    /** 请求 */
    public static final byte REQUEST = 0x01;
    /** 响应 */
    public static final byte RESPONSE = 0x02;
    /** 心跳请求 */
    public static final byte HEARTBEAT_REQUEST = 0x03;
    /** 心跳响应 */
    public static final byte HEARTBEAT_RESPONSE = 0x04;
    
    private MessageType() {
        // 工具类，不允许实例化
    }
}

