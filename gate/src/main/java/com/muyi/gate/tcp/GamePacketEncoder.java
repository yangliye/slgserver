package com.muyi.gate.tcp;

import com.muyi.common.util.codec.PacketCipher;
import com.muyi.proto.GamePacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * GamePacket 编码器
 * <p>
 * 将 GamePacket 编码为 ByteBuf。
 * 如果 Channel 上已设置 {@link PacketCipher}，自动加密 payload。
 * 由 LengthFieldPrepender 自动加上 4 字节 length 头。
 * <p>
 * 输出格式：
 * <pre>
 * +----------+----------+----------+
 * | msgId    | msgSeq   | payload  |
 * | 4 bytes  | 4 bytes  | N bytes  |
 * +----------+----------+----------+
 * </pre>
 */
public class GamePacketEncoder extends MessageToByteEncoder<GamePacket> {

    @Override
    protected void encode(ChannelHandlerContext ctx, GamePacket msg, ByteBuf out) {
        out.writeInt(msg.getMsgId());
        out.writeInt(msg.getMsgSeq());
        byte[] payload = msg.getPayload();
        if (payload != null && payload.length > 0) {
            PacketCipher cipher = ctx.channel().attr(CipherAttr.CIPHER).get();
            if (cipher != null) {
                cipher.encrypt(payload);
            }
            out.writeBytes(payload);
        }
    }
}
