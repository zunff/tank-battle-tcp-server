package com.zunf.tankbattletcpserver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Netty服务端配置类
 */
@Data
@Component
@ConfigurationProperties(prefix = "netty.server")
public class NettyServerConfig {
    // 监听端口
    private int port = 8888;
    // Boss线程数（处理连接请求）
    private int bossThreadCount = 1;
    // Worker线程数（处理IO读写）
    private int workerThreadCount = 8;
    // TCP参数：连接等待队列大小
    private int soBacklog = 1024;
    // TCP保活
    private boolean soKeepAlive = true;
    // 禁用Nagle算法（减少延迟）
    private boolean tcpNodelay = true;
    // 空闲检测时间（秒）：读空闲/写空闲/全空闲
    private int readerIdleTime = 5;
    private int writerIdleTime = 10;
    private int allIdleTime = 15;
}