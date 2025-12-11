package com.zunf.tankbattletcpserver.handler;

import com.zunf.tankbattletcpserver.constant.ProtocolConstant;
import com.zunf.tankbattletcpserver.entity.GameMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

@ChannelHandler.Sharable
public class GameMessageEncoder extends MessageToMessageEncoder<GameMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, GameMessage msg, List<Object> out) throws Exception {
        ByteBufAllocator alloc = ctx.alloc();

        byte[] body = msg.getBody();
        int bodyLength = (body != null ? body.length : 0);

        ByteBuf buf = alloc.buffer(ProtocolConstant.HEADER_TOTAL_LENGTH + bodyLength);

        // 写头部
        buf.writeByte(msg.getMsgType().getCode()); // type
        buf.writeByte(msg.getVersion());           // version
        buf.writeInt(bodyLength);                  // length
        buf.writeInt(0);                        // 先占位 CRC32，后面 ChecksumHandler 会填

        // 写 body
        if (bodyLength > 0) {
            buf.writeBytes(body);
        }

        out.add(buf);
    }
}
