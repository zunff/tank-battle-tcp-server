package com.zunf.tankbattletcpserver.handler;

import com.zunf.tankbattletcpserver.model.entity.game.GameMessage;
import com.zunf.tankbattletcpserver.enums.ErrorCode;
import com.zunf.tankbattletcpserver.enums.GameMsgType;
import com.zunf.tankbattletcpserver.manager.GameMatchManager;
import com.zunf.tankbattletcpserver.manager.GameRoomManager;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Component
public class MsgTypeHandlerRegister {

    @Resource
    private GameRoomManager gameRoomManager;

    @Resource
    private GameMatchManager gameMatchManager;

    private final Map<GameMsgType, Function<GameMessage, CompletableFuture<GameMessage>>> registry = new HashMap<>();

    @PostConstruct
    public void register() {
        registry.put(GameMsgType.CREATE_ROOM, wrap(gameRoomManager::createGameRoom));
        registry.put(GameMsgType.PAGE_ROOM, wrap(gameRoomManager::pageGameRoom));
        registry.put(GameMsgType.JOIN_ROOM, gameRoomManager::joinGameRoom);
        registry.put(GameMsgType.LEAVE_ROOM, gameRoomManager::leaveGameRoom);
        registry.put(GameMsgType.READY, gameRoomManager::ready);
        registry.put(GameMsgType.START_GAME, gameRoomManager::startGame);
        registry.put(GameMsgType.LOADED_ACK, gameRoomManager::loadedAck);

        registry.put(GameMsgType.TANK_SHOOT, wrap(gameMatchManager::handlerShoot));
        registry.put(GameMsgType.TANK_MOVE, wrap(gameMatchManager::handlerMove));
        registry.put(GameMsgType.LEAVE_GAME, wrap(gameMatchManager::handlerLeaveGame));
    }

    /**
     * 将一个【同步】的消息处理函数包装成返回 CompletableFuture 的异步接口。
     *
     * 使用场景：
     * - 适用于：方法签名为 {@code GameMessage xxx(GameMessage msg)} 的同步方法，
     *   且该方法：
     *   1. 只做轻量级的 CPU / 内存操作（如组装对象、简单校验、操作内存 Map 等）；
     *   2. 不访问外部服务（数据库、Redis、HTTP/RPC 等）；
     *   3. 不会进行长时间阻塞（如 Thread.sleep / IO 操作）。
     *
     * 包装后的行为：
     * - 被包装的方法仍然在【调用 handle(...) 的线程】中同步执行，
     *   对当前项目来说，就是在 Netty 的 I/O 线程中执行；
     * - `wrap` 只是把同步返回值用 {@link CompletableFuture#completedFuture(Object)}
     *   包装成一个“已完成”的 Future，方便统一用异步风格（`CompletableFuture<GameMessage>`）来处理；
     * - 不会切换线程，不会把任务丢到线程池中执行。
     *
     * 注意事项（非常重要）：
     * - 不要用来包装：
     *   1. 可能耗时较长的业务逻辑；
     *   2. 任何会阻塞线程的操作（包括数据库、网络 IO、RPC 调用等）；
     *   3. 需要在业务线程池中串行/并行调度的逻辑。
     *   否则，这些耗时逻辑会在 Netty I/O 线程中执行，导致 I/O 线程被占用，
     *   影响整个服务器的吞吐量和延迟。
     *
     * 如果某个方法是耗时的 / 需要访问外部服务：
     * - 请不要使用 wrap，而是：
     *   1. 直接让该方法返回 {@code CompletableFuture<GameMessage>}，在方法内部自行切线程；
     *   2. 或者在注册时使用 {@code CompletableFuture.supplyAsync(..., 业务线程池)}，
     *      显式把同步方法丢到业务线程池中执行。
     */
    private Function<GameMessage, CompletableFuture<GameMessage>> wrap(Function<GameMessage, GameMessage> function) {
        return (gameMessage) -> CompletableFuture.completedFuture(function.apply(gameMessage));
    }

    public CompletableFuture<GameMessage> handle(GameMessage inbound) {
        Function<GameMessage, CompletableFuture<GameMessage>> handler = registry.get(inbound.getMsgType());
        if (handler == null) {
            return CompletableFuture.completedFuture(GameMessage.fail(inbound, ErrorCode.UNSUPPORTED_COMMAND));
        }
        return handler.apply(inbound);
    }

}
