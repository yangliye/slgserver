package com.muyi.gate.tcp;

import com.muyi.common.util.codec.PacketCipher;
import io.netty.util.AttributeKey;

/**
 * Channel Attribute key for packet cipher.
 * <p>
 * 认证成功后由 {@link GateChannelHandler} 设置，
 * {@link GamePacketEncoder} 和 {@link GamePacketDecoder} 读取。
 */
public final class CipherAttr {

    public static final AttributeKey<PacketCipher> CIPHER = AttributeKey.valueOf("packetCipher");

    private CipherAttr() {
    }
}
