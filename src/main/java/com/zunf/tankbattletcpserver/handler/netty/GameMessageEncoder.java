package com.zunf.tankbattletcpserver.handler.netty;

import com.zunf.tankbattletcpserver.constant.ProtocolConstant;
import com.zunf.tankbattletcpserver.model.entity.game.GameMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ChannelHandler.Sharable
public class GameMessageEncoder extends MessageToMessageEncoder<GameMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, GameMessage msg, List<Object> out) throws Exception {
        ByteBufAllocator alloc = ctx.alloc();

        byte[] body = msg.getBody();
        int bodyLength = (body != null ? body.length : 0);

        ByteBuf buf = alloc.buffer(ProtocolConstant.HEADER_TOTAL_LENGTH + bodyLength);

        // 写头部
        buf.writeShort(msg.getMsgType().getCode());
        buf.writeByte(msg.getVersion());
        buf.writeInt(msg.getRequestId());
        buf.writeInt(bodyLength);
        // 先占位 CRC32，后面 ChecksumHandler 会填
        buf.writeInt(0);

        // 写 body
        if (bodyLength > 0) {
            buf.writeBytes(body);
        }

        out.add(buf);
    }
}
