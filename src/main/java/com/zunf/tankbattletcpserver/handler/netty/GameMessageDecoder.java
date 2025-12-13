package com.zunf.tankbattletcpserver.handler.netty;

import com.zunf.tankbattletcpserver.entity.GameMessage;
import com.zunf.tankbattletcpserver.enums.GameMsgType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ChannelHandler.Sharable
public class GameMessageDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        // 解析头部
        byte typeByte = buf.readByte();
        byte version = buf.readByte();
        int requestId = buf.readInt();
        int bodyLength = buf.readInt();
        int crc32 = buf.readInt(); // 这里可以不用了，只是把 readerIndex 往后挪

        // 读取 body
        byte[] body = new byte[bodyLength];
        buf.readBytes(body);

        // 组装 GameMessage
        GameMsgType msgType = GameMsgType.of(typeByte & 0xFF);
        GameMessage gameMessage = new GameMessage(msgType, version, requestId, body);

        out.add(gameMessage);
    }
}
