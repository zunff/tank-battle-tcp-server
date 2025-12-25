package com.zunf.tankbattletcpserver.executor;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 串行执行器
 *
 * @author zunf
 * @date 2025/12/25 21:55
 */
public class SerialExecutor {

    private final Queue<Runnable> tasks = new ArrayDeque<>();
    private final Executor backend;
    private Runnable active;

    public SerialExecutor(Executor backend) {
        this.backend = backend;
    }

    // 提交 Callable，返回 CompletableFuture
    public <T> CompletableFuture<T> submit(Callable<T> callable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        // 创建一个 Runnable，用于执行 Callable
        Runnable task = () -> {
            try {
                T result = callable.call();
                future.complete(result);
            } catch (Throwable e) {
                future.completeExceptionally(e);
            } finally {
                // 链式调用 调度下一个任务
                scheduleNext();
            }
        };
        // 添加任务
        synchronized (this) {
            tasks.offer(task);
            // 如果没有正在执行的任务，则调度下一个任务
            if (active == null) {
                scheduleNext();
            }
        }
        return future;
    }

    private synchronized void scheduleNext() {
        active = tasks.poll();
        if (active != null) {
            backend.execute(active);
        }
    }
}

