package com.zunf.tankbattletcpserver.config;

import com.zunf.tankbattletcpserver.handler.netty.*;
import com.zunf.tankbattletcpserver.manager.OnlineSessionManager;
import com.zunf.tankbattletcpserver.manager.grpc.AuthGrpcClient;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Netty服务端通道初始化器
 *
 * @author zunf
 */
@Component
public class NettyChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final LoggingHandler loggingHandler = new LoggingHandler(LogLevel.DEBUG);
    @Resource
    private ChecksumHandler checksumHandler;
    @Resource
    private GameMessageDecoder gameMessageDecoder;
    @Resource
    private GameMessageEncoder gameMessageEncoder;
    @Resource
    private GameDispatchHandler gameDispatchHandler;
    @Resource
    private OnlineSessionManager onlineSessionManager;
    @Resource
    private AuthGrpcClient authGrpcClient;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        // 按顺序添加各种 Handler 入站时按顺序执行 出站时按逆序执行
        p.addLast("loggingHandler", loggingHandler);
        p.addLast("frameDecoder", new FrameDecoder());
        p.addLast("checksumHandler", checksumHandler);
        p.addLast("gameMessageDecoder", gameMessageDecoder);
        p.addLast("gameMessageEncoder", gameMessageEncoder);
        p.addLast("sessionHandler", new SessionHandler(onlineSessionManager, authGrpcClient));
        p.addLast("gameDispatchHandler", gameDispatchHandler);
    }
}
