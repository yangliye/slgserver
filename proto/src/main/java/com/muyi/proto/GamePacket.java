package com.muyi.proto;

/**
 * 客户端 TCP 包封装
 * <p>
 * Gate 解码后的中间表示，持有原始字节不立即解析 protobuf。
 * <p>
 * 帧格式（length 由 Netty LengthField 处理，此类不包含）：
 * <pre>
 * +----------+----------+----------+
 * | msgId    | msgSeq   | payload  |
 * | 4 bytes  | 4 bytes  | N bytes  |
 * +----------+----------+----------+
 * </pre>
 */
public class GamePacket {

    private final int msgId;
    private final int msgSeq;
    private final byte[] payload;

    public GamePacket(int msgId, int msgSeq, byte[] payload) {
        this.msgId = msgId;
        this.msgSeq = msgSeq;
        this.payload = payload;
    }

    public int getMsgId() {
        return msgId;
    }

    public int getMsgSeq() {
        return msgSeq;
    }

    public byte[] getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "GamePacket{msgId=" + msgId
                + ", msgSeq=" + msgSeq
                + ", payloadLen=" + (payload != null ? payload.length : 0) + '}';
    }
}
