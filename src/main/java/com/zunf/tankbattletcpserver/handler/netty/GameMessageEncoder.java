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
        ByteBuf buf = null;
        try {
            byte[] body = msg.getBody();
            int bodyLength = (body != null ? body.length : 0);

            // 分配缓冲区
            buf = alloc.buffer(ProtocolConstant.HEADER_TOTAL_LENGTH + bodyLength);

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

            // 正常场景：添加到out，由Netty接管释放
            out.add(buf);
            // 标记为已移交，避免finally重复释放
            buf = null;
        } finally {
            // 异常场景：buf未被添加到out，手动释放
            if (buf != null) {
                buf.release();
            }
        }
    }
}
