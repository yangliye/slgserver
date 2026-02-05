package com.muyi.rpc.protocol;

/**
 * RPC消息封装
 *
 * @author muyi
 */
public class RpcMessage {
    
    /** 消息类型 */
    private byte messageType;
    
    /** 序列化类型 */
    private byte serializeType;
    
    /** 压缩类型 */
    private byte compressType;
    
    /** 消息ID */
    private long messageId;
    
    /** 消息体 */
    private Object data;
    
    public RpcMessage() {
    }
    
    public RpcMessage(byte messageType, byte serializeType, long messageId, Object data) {
        this(messageType, serializeType, (byte) 0, messageId, data);
    }
    
    public RpcMessage(byte messageType, byte serializeType, byte compressType, long messageId, Object data) {
        this.messageType = messageType;
        this.serializeType = serializeType;
        this.compressType = compressType;
        this.messageId = messageId;
        this.data = data;
    }
    
    // Getters and Setters
    
    public byte getMessageType() {
        return messageType;
    }
    
    public void setMessageType(byte messageType) {
        this.messageType = messageType;
    }
    
    public byte getSerializeType() {
        return serializeType;
    }
    
    public void setSerializeType(byte serializeType) {
        this.serializeType = serializeType;
    }
    
    public byte getCompressType() {
        return compressType;
    }
    
    public void setCompressType(byte compressType) {
        this.compressType = compressType;
    }
    
    public long getMessageId() {
        return messageId;
    }
    
    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
}

