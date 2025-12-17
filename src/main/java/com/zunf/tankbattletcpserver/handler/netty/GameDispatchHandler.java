package com.zunf.tankbattletcpserver.handler.netty;

import cn.hutool.core.lang.UUID;
import com.zunf.tankbattletcpserver.entity.GameMessage;
import com.zunf.tankbattletcpserver.handler.MsgTypeHandlerRegister;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@ChannelHandler.Sharable
public class GameDispatchHandler extends SimpleChannelInboundHandler<GameMessage> {

    @Resource
    private MsgTypeHandlerRegister handlerRegister;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GameMessage msg) throws Exception {
        // 生成 traceId
        msg.setTraceId(UUID.randomUUID().toString(true));
        // 根据消息类型进行分发，grpc调用业务端
        GameMessage outbound = handlerRegister.handle(msg);
        ctx.writeAndFlush(outbound);
    }
}