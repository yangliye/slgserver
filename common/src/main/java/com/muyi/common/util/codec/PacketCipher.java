package com.muyi.common.util.codec;

/**
 * 数据包加解密接口
 * <p>
 * 双向流加密：维护独立的发送/接收密钥流状态，
 * 保证同一方向的包按顺序加解密。
 * <p>
 * 只加密 payload 部分，msgId/msgSeq 明文传输（路由层需要读取）。
 */
public interface PacketCipher {

    /**
     * 加密（发送方向）
     * <p>
     * 原地加密，返回值即 data 本身（避免拷贝）。
     * 调用顺序必须与对端 decrypt 顺序一致。
     */
    byte[] encrypt(byte[] data);

    /**
     * 解密（接收方向）
     * <p>
     * 原地解密，返回值即 data 本身。
     */
    byte[] decrypt(byte[] data);
}
