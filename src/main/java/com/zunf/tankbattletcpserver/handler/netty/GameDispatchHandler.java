package com.zunf.tankbattletcpserver.handler.netty;

import cn.hutool.core.lang.UUID;
import com.zunf.tankbattletcpserver.common.BusinessException;
import com.zunf.tankbattletcpserver.model.entity.game.GameMessage;
import com.zunf.tankbattletcpserver.enums.ErrorCode;
import com.zunf.tankbattletcpserver.handler.MsgTypeHandlerRegister;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@ChannelHandler.Sharable
public class GameDispatchHandler extends SimpleChannelInboundHandler<GameMessage> {

    @Resource
    private MsgTypeHandlerRegister handlerRegister;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GameMessage msg) throws Exception {
        // 生成 traceId
        msg.setTraceId(UUID.randomUUID().toString(true));
        // 根据消息类型进行分发
        CompletableFuture<GameMessage> future = handlerRegister.handle(msg);

        future.whenComplete((outbound, throwable) -> {
            // 这里可以做统一异常处理，返回一个错误响应
            if (throwable != null) {
                GameMessage errorResp;
                if (throwable instanceof BusinessException bizEx) {
                    // 自定义的异常，返回一个错误响应
                    errorResp = GameMessage.fail(msg, ErrorCode.of(bizEx.getCode(), ErrorCode.INTERNAL_ERROR));
                } else {
                    // 不是自定义的异常，返回一个通用错误响应
                    errorResp = GameMessage.fail(msg, ErrorCode.INTERNAL_ERROR);
                }
                log.error("Failed to handle message {}", msg, throwable);
                ctx.writeAndFlush(errorResp);
                return;
            }
            // 响应成功 返回业务数据
            if (outbound != null) {
                ctx.writeAndFlush(outbound);
            }
        });
    }
}