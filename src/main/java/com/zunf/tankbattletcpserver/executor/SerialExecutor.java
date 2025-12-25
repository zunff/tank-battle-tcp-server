package com.zunf.tankbattletcpserver.executor;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class SerialExecutor implements Executor {

    private final Queue<Runnable> tasks = new ArrayDeque<>();
    private final Executor backend;
    private Runnable active;

    public SerialExecutor(Executor backend) {
        this.backend = backend;
    }

    @Override
    public synchronized void execute(Runnable command) {
        tasks.offer(wrap(command));
        if (active == null) {
            scheduleNext();
        }
    }

    // 提交 Callable，返回 CompletableFuture
    public <T> CompletableFuture<T> submit(Callable<T> callable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Runnable task = () -> {
            try {
                T result = callable.call();
                future.complete(result);
            } catch (Throwable e) {
                future.completeExceptionally(e);
            } finally {
                scheduleNext();
            }
        };
        synchronized (this) {
            tasks.offer(task);
            if (active == null) {
                scheduleNext();
            }
        }
        return future;
    }

    private Runnable wrap(Runnable command) {
        return () -> {
            try {
                command.run();
            } finally {
                scheduleNext();
            }
        };
    }

    private synchronized void scheduleNext() {
        active = tasks.poll();
        if (active != null) {
            backend.execute(active);
        }
    }
}

