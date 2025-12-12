package com.zunf.tankbattletcpserver.handler.netty;

import cn.hutool.core.lang.UUID;
import com.zunf.tankbattletcpserver.entity.GameMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.stereotype.Component;

@Component
@ChannelHandler.Sharable
public class GameDispatchHandler extends SimpleChannelInboundHandler<GameMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GameMessage msg) throws Exception {
        // 生成 traceId
        msg.setTraceId(UUID.randomUUID().toString(true));
        // todo 根据消息类型进行分发，grpc调用业务端
    }
}