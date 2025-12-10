package com.zunf.tankbattletcpserver.server;


import com.zunf.tankbattletcpserver.config.NettyServerConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Netty TCP Server核心类（Spring Bean，管理生命周期）
 */
@Slf4j
@Component
public class NettyTcpServer {

    @Resource
    private NettyServerConfig nettyConfig;

    /**
     * Netty核心线程组（需优雅关闭）
     */
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    /**
     * 通道未来对象（用于关闭服务）
     */
    private ChannelFuture channelFuture;

    /**
     * Spring容器初始化完成后启动Netty服务（@PostConstruct）
     */
    @PostConstruct
    public void start() throws InterruptedException {
        // 1. 初始化线程组
        bossGroup = new NioEventLoopGroup(nettyConfig.getBossThreadCount());
        workerGroup = new NioEventLoopGroup(nettyConfig.getWorkerThreadCount());

        // 2. 创建ServerBootstrap（服务端启动器）
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap
                // 绑定线程组
                .group(bossGroup, workerGroup)
                // 指定NIO通道类型
                .channel(NioServerSocketChannel.class)
                // 服务端通道参数配置
                .option(ChannelOption.SO_BACKLOG, nettyConfig.getSoBacklog())
                .option(ChannelOption.SO_REUSEADDR, true) // 端口复用
                .handler(new LoggingHandler(LogLevel.INFO)) // Boss线程组日志
                // 客户端通道参数配置
                .childOption(ChannelOption.SO_KEEPALIVE, nettyConfig.isSoKeepAlive())
                .childOption(ChannelOption.TCP_NODELAY, nettyConfig.isTcpNodelay())
                // 初始化客户端通道的处理器链
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        // 按顺序添加各种 Handler 入站时按顺序执行 出站时按逆序执行
                        p.addLast( new LoggingHandler(LogLevel.DEBUG));
                    }
                });

        // 3. 绑定端口并启动服务（sync()阻塞等待绑定完成）
        channelFuture = serverBootstrap.bind(nettyConfig.getPort()).sync();
        log.info("Netty TCP Server启动成功，监听端口：{}", nettyConfig.getPort());

        // 4. 阻塞等待服务关闭（可选，Spring容器关闭时会触发@PreDestroy）
        // channelFuture.channel().closeFuture().sync();
    }

    /**
     * Spring容器销毁前优雅关闭Netty服务（@PreDestroy）
     */
    @PreDestroy
    public void stop() throws InterruptedException {
        log.info("开始关闭Netty TCP Server...");
        // 1. 关闭通道
        if (channelFuture != null) {
            channelFuture.channel().close().sync();
        }
        // 2. 优雅关闭线程组（释放资源）
        if (workerGroup != null) {
            workerGroup.shutdownGracefully().sync();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully().sync();
        }
        log.info("Netty TCP Server已优雅关闭");
    }
}