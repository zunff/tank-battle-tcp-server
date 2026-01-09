package com.zunf.tankbattletcpserver.manager;

import cn.hutool.core.collection.CollUtil;
import com.google.protobuf.ByteString;
import com.zunf.tankbattletcpserver.common.BusinessException;
import com.zunf.tankbattletcpserver.model.entity.game.GameMatch;
import com.zunf.tankbattletcpserver.model.entity.game.GameMessage;
import com.zunf.tankbattletcpserver.model.entity.game.GameRoom;
import com.zunf.tankbattletcpserver.model.entity.PlayerInMatch;
import com.zunf.tankbattletcpserver.enums.ErrorCode;
import com.zunf.tankbattletcpserver.enums.GameMsgType;
import com.zunf.tankbattletcpserver.enums.MatchStatus;
import com.zunf.tankbattletcpserver.grpc.game.match.MatchClientProto;
import com.zunf.tankbattletcpserver.grpc.game.room.GameRoomClientProto;
import com.zunf.tankbattletcpserver.handler.MapGenerateHandler;
import com.zunf.tankbattletcpserver.util.ProtoBufUtil;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Slf4j
@Component
public class GameMatchManager {

    @Resource
    private MapGenerateHandler mapGenerateHandler;

    @Resource
    private ScheduledExecutorService scheduledExecutor;

    @Resource
    private ExecutorService gameExecutor;

    @Resource
    private OnlineSessionManager onlineSessionManager;

    AtomicLong atomicInteger = new AtomicLong(0);

    private final Map<Long, GameMatch> gameMatchMap = new ConcurrentHashMap<>();

    public Long generateMatchId() {
        return atomicInteger.incrementAndGet();
    }

    public GameMatch createGameMatch(GameRoom room, Consumer<Long> roomMatchEndCallback) {
        if (room.getRoomStatus() != GameRoomClientProto.RoomStatus.WAITING) {
            throw new BusinessException(ErrorCode.GAME_ROOM_STATUS_ERROR);
        }
        Long matchId = generateMatchId();
        GameMatch gameMatch = new GameMatch(matchId, room.getRoomId(), mapGenerateHandler.generateMap(room.getMaxPlayer())
                , room.getMaxPlayer(), 60 * 10, room.getCurPlayers().stream().map(PlayerInMatch::new).toList()
                , this::asyncPushTickToClients,  roomMatchEndCallback);
        gameMatchMap.put(matchId, gameMatch);
        return gameMatch;
    }

    public void startGame(Long matchId) {
        GameMatch gameMatch = gameMatchMap.get(matchId);
        if (gameMatch == null) {
            throw new BusinessException(ErrorCode.GAME_MATCH_NOT_FOUND);
        }
        gameMatch.setStatus(MatchStatus.RUNNING);
        gameMatch.setStartTime(System.currentTimeMillis());
        // 初始化 tick
        gameMatch.initTick();
        //  启动周期性 tick 任务
        ScheduledFuture<?> future = scheduledExecutor.scheduleAtFixedRate(
                gameMatch::tick,
                0, // - 第二个参数：初始延迟（0ms，创建后立即执行第一次tick）
                50,          // - 第三个参数：周期时间（ms，每次tick间隔）
                TimeUnit.MILLISECONDS
        );
        gameMatch.setTickTask(future);
        log.info("Match started: {}", gameMatch.getMatchId());
    }

    /**
     * 异步推送 Tick 状态给所有客户端
     */
    public void asyncPushTickToClients(GameMatch gameMatch) {
        if (gameMatch == null || CollUtil.isEmpty(gameMatch.getPlayers())) {
            return;
        }
        // 异步推送
        gameExecutor.execute(() -> {
            for (PlayerInMatch player : gameMatch.getPlayers()) {
                if (!player.getOnline()) {
                    continue;
                }
                onlineSessionManager.pushToPlayer(player.getPlayerId(), GameMessage.success(GameMsgType.GAME_TICK, gameMatch.getLatestTick().toProto().toByteString()));
            }
        });
        log.info("Match tick pushed: {}", gameMatch.getMatchId());
    }

    public @NonNull GameMatch getGameMatch(Long matchId, boolean checkRunning) {
        if (matchId == null) {
            throw new BusinessException(ErrorCode.GAME_MATCH_NOT_FOUND);
        }
        GameMatch gameMatch = gameMatchMap.get(matchId);
        if (gameMatch == null) {
            throw new BusinessException(ErrorCode.GAME_MATCH_NOT_FOUND);
        }
        if (checkRunning && gameMatch.getStatus() != MatchStatus.RUNNING) {
            throw new BusinessException(ErrorCode.GAME_MATCH_STATUS_ERROR);
        }
        return gameMatch;
    }

    public GameMessage handlerShoot(GameMessage inbound) {
        MatchClientProto.OpRequest request = ProtoBufUtil.parseBytes(inbound.getBody(), MatchClientProto.OpRequest.parser());
        GameMatch gameMatch = getGameMatch(request.getMatchId(), true);
        gameMatch.offerOperation(GameMsgType.TANK_SHOOT, request.getPlayerId(), request.getOpParams());
        return GameMessage.success(inbound, ByteString.EMPTY);
    }

    public GameMessage handlerMove(GameMessage inbound) {
        MatchClientProto.OpRequest request = ProtoBufUtil.parseBytes(inbound.getBody(), MatchClientProto.OpRequest.parser());
        GameMatch gameMatch = getGameMatch(request.getMatchId(), true);
        gameMatch.offerOperation(GameMsgType.TANK_MOVE, request.getPlayerId(), request.getOpParams());
        return GameMessage.success(inbound, ByteString.EMPTY);
    }

    public GameMessage handlerLeaveGame(GameMessage inbound) {
        MatchClientProto.LeaveMatchReq request = ProtoBufUtil.parseBytes(inbound.getBody(), MatchClientProto.LeaveMatchReq.parser());
        GameMatch gameMatch = getGameMatch(request.getMatchId(), false);
        List<PlayerInMatch> players = gameMatch.getPlayers();
        players.stream().filter(playerInMatch -> playerInMatch.getPlayerId().equals(request.getPlayerId())).findFirst().ifPresent(playerInMatch -> {
            playerInMatch.setOnline(false);
        });
        return GameMessage.success(inbound, ByteString.EMPTY);
    }
}
