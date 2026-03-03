package com.muyi.gate.tcp;

import com.muyi.common.util.codec.PacketCipher;
import com.muyi.proto.GamePacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

/**
 * GamePacket 解码器
 * <p>
 * 将 LengthFieldBasedFrameDecoder 输出的 ByteBuf 解码为 GamePacket。
 * 如果 Channel 上已设置 {@link PacketCipher}，自动解密 payload。
 * <p>
 * 输入帧格式（length 头已由 LengthFieldBasedFrameDecoder 剥离）：
 * <pre>
 * +----------+----------+----------+
 * | msgId    | msgSeq   | payload  |
 * | 4 bytes  | 4 bytes  | N bytes  |
 * +----------+----------+----------+
 * </pre>
 */
public class GamePacketDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
        if (msg.readableBytes() < 8) {
            return;
        }
        int msgId = msg.readInt();
        int msgSeq = msg.readInt();
        byte[] payload = new byte[msg.readableBytes()];
        msg.readBytes(payload);

        PacketCipher cipher = ctx.channel().attr(CipherAttr.CIPHER).get();
        if (cipher != null && payload.length > 0) {
            cipher.decrypt(payload);
        }

        out.add(new GamePacket(msgId, msgSeq, payload));
    }
}
