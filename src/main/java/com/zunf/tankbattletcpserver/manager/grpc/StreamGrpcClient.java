package com.zunf.tankbattletcpserver.manager.grpc;

import com.google.protobuf.ByteString;
import com.zunf.tankbattletcpserver.entity.GameMessage;
import com.zunf.tankbattletcpserver.enums.GameMsgType;
import com.zunf.tankbattletcpserver.grpc.server.stream.PushServiceGrpc;
import com.zunf.tankbattletcpserver.grpc.server.stream.StreamProto;
import com.zunf.tankbattletcpserver.manager.OnlineSessionManager;
import com.zunf.tankbattletcpserver.util.ProtoBufUtil;
import io.grpc.stub.StreamObserver;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class StreamGrpcClient {

    @GrpcClient("tank-battle-backend")
    private PushServiceGrpc.PushServiceStub pushService;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        // SpringBoot 启动完成（所有 Bean 初始化完、应用已 ready）后调用
        subscribe();
    }

    @PreDestroy
    public void onShutdown() {
        // Spring 容器关闭时调用（优雅停机）
        stop();
    }

    @Resource
    private OnlineSessionManager onlineSessionManager;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "grpc-subscribe-reconnect");
                t.setDaemon(true);
                return t;
            });

    private final AtomicBoolean running = new AtomicBoolean(false);

    // 保存当前 call，便于 stop 时 cancel
    private final AtomicReference<io.grpc.stub.ClientCallStreamObserver<StreamProto.SubscribeRequest>> requestStreamRef = new AtomicReference<>();

    private volatile long backoffMs = 1000;  // 当前重连等待时间（初始 1s）
    private final long maxBackoffMs = 30_000; // 最大等待时间（最多 30s）

    /** 启动订阅（建议应用启动后调用一次） */
    public void subscribe() {
        if (!running.compareAndSet(false, true)) return;
        doSubscribe();
    }

    /** 停止订阅 */
    public void stop() {
        running.set(false);

        var reqObs = requestStreamRef.getAndSet(null);
        if (reqObs != null) {
            reqObs.cancel("client stop", null);
        }
        scheduler.shutdownNow();
    }

    private void doSubscribe() {
        if (!running.get()) return;

        StreamProto.SubscribeRequest req = StreamProto.SubscribeRequest.newBuilder().setTcpServerId("tcpserver-1").build();

        log.info("gRPC subscribe start...");

        StreamObserver<StreamProto.PushMessage> respObserver = new StreamObserver<>() {
            @Override
            public void onNext(StreamProto.PushMessage msg) {
                // 收到服务端推送
                handle(msg);
            }

            @Override
            public void onError(Throwable t) {
                log.warn("gRPC subscribe error, will reconnect: {}", t.toString());
                requestStreamRef.set(null);
                scheduleReconnect();
            }

            @Override
            public void onCompleted() {
                log.warn("gRPC subscribe completed, will reconnect...");
                requestStreamRef.set(null);
                scheduleReconnect();
            }
        };

        // 关键：用 ClientResponseObserver 拿到 ClientCallStreamObserver，从而支持 cancel/流控
        io.grpc.stub.ClientResponseObserver<StreamProto.SubscribeRequest, StreamProto.PushMessage> clientRespObserver =
                new io.grpc.stub.ClientResponseObserver<>() {

                    @Override
                    public void beforeStart(io.grpc.stub.ClientCallStreamObserver<StreamProto.SubscribeRequest> requestStream) {
                        requestStreamRef.set(requestStream);

                        // server-streaming：客户端只发一次请求就够了
                        // 这里也可以设置流控（可选）
                        // requestStream.disableAutoInboundFlowControl();
                        // requestStream.setOnReadyHandler(() -> { ... });

                        // 这里不需要 onNext，因为 subscribe 是 unary request（只发一个 req）
                    }

                    @Override
                    public void onNext(StreamProto.PushMessage value) {
                        respObserver.onNext(value);
                    }

                    @Override
                    public void onError(Throwable t) {
                        respObserver.onError(t);
                    }

                    @Override
                    public void onCompleted() {
                        respObserver.onCompleted();
                    }
                };

        try {
            // 发起订阅（非阻塞）
            pushService.subscribe(req, clientRespObserver);

            // 连接成功后重置退避（严格来说：这里不代表一定成功握手，但够用）
            backoffMs = 1000;

        } catch (Exception e) {
            log.warn("gRPC subscribe start failed, will reconnect: {}", e.toString());
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (!running.get()) return;

        long delay = backoffMs;
        backoffMs = Math.min(maxBackoffMs, backoffMs * 2);

        scheduler.schedule(this::doSubscribe, delay, TimeUnit.MILLISECONDS);
        log.info("scheduled reconnect in {} ms", delay);
    }

    private void handle(StreamProto.PushMessage msg) {
        long playerId = msg.getPlayerId();
        int msgType = msg.getMsgType();
        byte[] payload = msg.getPayload().toByteArray();

        Channel channel = onlineSessionManager.getChannel(playerId);
        if (channel == null) {
            return;
        }
        channel.writeAndFlush(new GameMessage(GameMsgType.of(msgType), ProtoBufUtil.successResp(ByteString.copyFrom(payload))));
    }
}
