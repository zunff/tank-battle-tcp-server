package com.zunf.tankbattletcpserver.handler.netty;

import com.zunf.tankbattletcpserver.AuthProto;
import com.zunf.tankbattletcpserver.entity.GameMessage;
import com.zunf.tankbattletcpserver.entity.SessionInfo;
import com.zunf.tankbattletcpserver.enums.GameMsgType;
import com.zunf.tankbattletcpserver.handler.message.LoginMessageHandler;
import com.zunf.tankbattletcpserver.manager.OnlineSessionManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;

public class SessionHandler extends ChannelInboundHandlerAdapter {

    private static final AttributeKey<SessionInfo> SESSION_KEY = AttributeKey.valueOf("SESSION_INFO");

    private final OnlineSessionManager onlineSessionManager;

    private final LoginMessageHandler loginMessageHandler = new LoginMessageHandler();

    public SessionHandler(OnlineSessionManager onlineSessionManager) {
        this.onlineSessionManager = onlineSessionManager;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        SessionInfo session = new SessionInfo();
        ctx.channel().attr(SESSION_KEY).set(session);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel ch = ctx.channel();
        SessionInfo session = ch.attr(SESSION_KEY).get();
        if (session != null && session.getPlayerId() != null) {
            long playerId = session.getPlayerId();
            // 从在线表中移除
            onlineSessionManager.unbind(playerId, ch);
        }
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof GameMessage gameMessage)) {
            super.channelRead(ctx, msg);
            return;
        }

        SessionInfo session = ctx.channel().attr(SESSION_KEY).get();
        if (session == null) {
            session = new SessionInfo();
            ctx.channel().attr(SESSION_KEY).set(session);
        }

        // 处理登录消息
        if (!session.isAuthenticated() && gameMessage.getMsgType() == GameMsgType.LOGIN) {
            // 1. 解析 body -> LoginRequest，grpc调用业务端鉴权
            Long playerId = loginMessageHandler.handle(gameMessage);
            if (playerId == null) {
                ctx.writeAndFlush(AuthProto.LoginResponse.newBuilder().setCode(-1).build().toByteArray());
            }

            // 2. 登录成功，更新 SessionInfo
            session.setPlayerId(playerId);
            session.setAuthenticated(true);

            // 3. 注册到 OnlineSessionManager
            onlineSessionManager.bind(playerId, ctx.channel());

            // 4. 构建并返回登录成功消息
             ctx.writeAndFlush(AuthProto.LoginResponse.newBuilder().setCode(0).setPlayerId(playerId).build().toByteArray());
            return;
        }

        // 非登录消息，要求已经鉴权
        if (!session.isAuthenticated()) {
            ctx.close();
            return;
        }

        // 把 playerId 写入 GameMessage，方便后续 Handler 使用
        if (session.getPlayerId() != null) {
            gameMessage.setPlayerId(session.getPlayerId());
        }

        super.channelRead(ctx, gameMessage);
    }
}
