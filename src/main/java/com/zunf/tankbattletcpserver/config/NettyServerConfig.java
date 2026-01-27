package com.zunf.tankbattletcpserver.config;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Netty服务端配置类
 *
 * @author zunf
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "netty.server")
public class NettyServerConfig {
    static {
        // 开发/测试阶段启用内存泄漏检测，生产改为LEVEL1
        System.setProperty("io.netty.leakDetection.level", "PARANOID");
        // 限制堆外内存上限，避免OOM
        System.setProperty("io.netty.maxDirectMemory", "128m");
    }

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


    @Bean
    public ByteBufAllocator pooledByteBufAllocator() {
        // 坦克大战场景参数适配：
        boolean preferDirect = true; // 优先堆外内存，减少IO拷贝
        int nHeapArena = Runtime.getRuntime().availableProcessors() * 2; // 堆内存池数量
        int nDirectArena = Runtime.getRuntime().availableProcessors() * 2; // 直接内存池数量
        int pageSize = 8192; // 8K，匹配小数据包
        int maxOrder = 11; // Chunk=8K*2^11=16M
        int smallCacheSize = 512; // 小内存块缓存数（调大适配高频小数据包）
        int normalCacheSize = 128; // 中等内存块缓存数
        boolean useCacheForAllThreads = true; // 所有线程共享缓存（提升复用率）
        int directMemoryCacheAlignment = 0; // 直接内存对齐（0=不开启，游戏场景无需对齐）

        // 正确使用九参数构造函数（注意参数类型和顺序！）
        return new PooledByteBufAllocator(
                preferDirect,
                nHeapArena,
                nDirectArena,
                pageSize,
                maxOrder,
                smallCacheSize,
                normalCacheSize,
                useCacheForAllThreads,
                directMemoryCacheAlignment
        );
    }
}