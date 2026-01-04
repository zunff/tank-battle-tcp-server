package com.zunf.tankbattletcpserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class ExecutorConfig {

    /**
     * 用作各房间 SerialExecutor 的 串行操作
     */
    @Bean(name = "gameExecutor")
    public ExecutorService gameExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();

        int corePoolSize = cores;
        int maximumPoolSize = cores * 2;
        long keepAliveTime = 60L;
        TimeUnit unit = TimeUnit.SECONDS;

        int queueCapacity = 10000;
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(queueCapacity);

        ThreadFactory threadFactory = new ThreadFactory() {
            private final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
            private final String namePrefix = "game-backend-";
            private int counter = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread t = defaultFactory.newThread(r);
                t.setName(namePrefix + counter++);
                // 根据需要设置
                // t.setDaemon(true);
                return t;
            }
        };

        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();

        return new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                unit,
                workQueue,
                threadFactory,
                handler
        );
    }

    @Bean
    public ScheduledExecutorService scheduledExecutor() {
        return Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    }
}