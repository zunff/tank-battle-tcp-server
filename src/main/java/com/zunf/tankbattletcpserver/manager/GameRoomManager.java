package com.zunf.tankbattletcpserver.manager;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.google.protobuf.ByteString;
import com.zunf.tankbattletcpserver.common.BusinessException;
import com.zunf.tankbattletcpserver.enums.ErrorCode;
import com.zunf.tankbattletcpserver.enums.GameMsgType;
import com.zunf.tankbattletcpserver.enums.MatchStatus;
import com.zunf.tankbattletcpserver.executor.SerialExecutor;
import com.zunf.tankbattletcpserver.grpc.CommonProto;
import com.zunf.tankbattletcpserver.grpc.game.match.MatchClientProto;
import com.zunf.tankbattletcpserver.grpc.game.room.GameRoomClientProto;
import com.zunf.tankbattletcpserver.grpc.server.user.UserProto;
import com.zunf.tankbattletcpserver.grpc.server.user.UserServiceGrpc;
import com.zunf.tankbattletcpserver.handler.MapGenerateHandler;
import com.zunf.tankbattletcpserver.model.entity.PlayerInMatch;
import com.zunf.tankbattletcpserver.model.entity.game.GameMatch;
import com.zunf.tankbattletcpserver.model.entity.game.GameMessage;
import com.zunf.tankbattletcpserver.model.entity.game.GameRoom;
import com.zunf.tankbattletcpserver.model.entity.game.GameRoomPlayer;
import com.zunf.tankbattletcpserver.util.ProtoBufUtil;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class GameRoomManager {

    @GrpcClient("tank-battle-backend")
    private UserServiceGrpc.UserServiceBlockingStub userService;

    @Resource
    private MapGenerateHandler mapGenerateHandler;

    @Resource
    private OnlineSessionManager onlineSessionManager;

    @Resource
    private ExecutorService gameExecutor;

    @Resource
    private ScheduledExecutorService scheduledExecutor;

    AtomicLong atomicInteger = new AtomicLong(0);

    Map<Long, GameRoom> gameRoomMap = new ConcurrentHashMap<>();


    private GameRoom getGameRoom(long roomId) {
        GameRoom gameRoom = gameRoomMap.get(roomId);
        if (gameRoom == null) {
            throw new BusinessException(ErrorCode.GAME_ROOM_NOT_FOUND);
        }
        return gameRoom;
    }

    /**
     * 在房间内的操作 串行
     *
     * @param roomId 房间id
     * @param task   要执行的逻辑
     */
    public <T> CompletableFuture<T> inRoomAsync(long roomId, Callable<T> task) {
        GameRoom room = gameRoomMap.get(roomId);
        if (room == null) {
            throw new BusinessException(ErrorCode.GAME_ROOM_NOT_FOUND);
        }
        SerialExecutor executor = room.getSerialExecutor();
        return executor.submit(task);
    }


    public GameMessage createGameRoom(GameMessage inbound) {
        GameRoomClientProto.CreateRequest req = ProtoBufUtil.parseBytes(inbound.getBody(), GameRoomClientProto.CreateRequest.parser());
        long roomId = atomicInteger.incrementAndGet();
        long creatorId = req.getPlayerId();
        GameRoom gameRoom = new GameRoom(roomId, creatorId, req.getName(), req.getMaxPlayers(), new SerialExecutor(gameExecutor));
        // 创建者自动加入房间
        UserProto.UserInfo userInfo = ProtoBufUtil.parseRespBody(userService.getUser(UserProto.GetUserRequest.newBuilder().setPlayerId(creatorId).build())
                , UserProto.GetUserResponse.parser()).getUser();
        if (ObjUtil.isNull(userInfo)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        gameRoom.addPlayer(creatorId, userInfo.getNickname());
        gameRoomMap.put(roomId, gameRoom);
        return GameMessage.success(inbound, GameRoomClientProto.CreateResponse.newBuilder().setRoomId(roomId).build().toByteString());
    }
 
    public GameMessage pageGameRoom(GameMessage inbound) {
        GameRoomClientProto.PageRequest clientRequest = ProtoBufUtil.parseBytes(inbound.getBody(), GameRoomClientProto.PageRequest.parser());
        List<GameRoom> gameRoomList = gameRoomMap.values().stream()
                .skip((long) (clientRequest.getPageNum() - 1) * clientRequest.getPageSize())
                .limit(clientRequest.getPageSize()).toList();
        List<GameRoomClientProto.GameRoomData> roomList = gameRoomList.stream().map(gameRoomBo -> GameRoomClientProto.GameRoomData.newBuilder()
                .setId(gameRoomBo.getRoomId())
                .setName(gameRoomBo.getRoomName())
                .setMaxPlayers(gameRoomBo.getMaxPlayer())
                .setNowPlayers(gameRoomBo.getCurPlayers().size())
                .setStatus(gameRoomBo.getRoomStatus()).build()).toList();
        return GameMessage.success(inbound, GameRoomClientProto.PageResponse.newBuilder().addAllData(roomList).setTotal(gameRoomMap.size()).build().toByteString());
    }

    public CompletableFuture<GameMessage> joinGameRoom(GameMessage inbound) {
        GameRoomClientProto.JoinRequest req = ProtoBufUtil.parseBytes(inbound.getBody(), GameRoomClientProto.JoinRequest.parser());
        long roomId = req.getRoomId();
        return inRoomAsync(roomId, () -> {
            long playerId = req.getPlayerId();
            GameRoom gameRoom = getGameRoom(roomId);
            if (gameRoom.containsPlayer(playerId)) {
                throw new BusinessException(ErrorCode.GAME_ROOM_PLAYER_EXIST);
            }
            if (gameRoom.isFull()) {
                throw new BusinessException(ErrorCode.GAME_ROOM_FULL);
            }
            if (gameRoom.getRoomStatus() != GameRoomClientProto.RoomStatus.WAITING) {
                throw new BusinessException(ErrorCode.GAME_ROOM_STATUS_ERROR, "房间已开始");
            }
            // 请求后端查询玩家信息
            UserProto.UserInfo userInfo = ProtoBufUtil.parseRespBody(userService.getUser(UserProto.GetUserRequest.newBuilder().setPlayerId(playerId).build()),
                    UserProto.GetUserResponse.parser()).getUser();
            if (ObjUtil.isNull(userInfo)) {
                throw new BusinessException(ErrorCode.NOT_FOUND);
            }
            gameRoom.addPlayer(playerId, userInfo.getNickname());

            // 构建房间详情
            List<GameRoomClientProto.PlayerInfo> playerDataList = gameRoom.getCurPlayers().stream().map(user -> GameRoomClientProto.PlayerInfo.newBuilder()
                    .setPlayerId(user.getId()).setNickName(user.getName()).build()).toList();
            GameRoomClientProto.GameRoomDetail gameRoomDetail = GameRoomClientProto.GameRoomDetail.newBuilder()
                    .setId(gameRoom.getRoomId())
                    .setName(gameRoom.getRoomName())
                    .setMaxPlayers(gameRoom.getMaxPlayer())
                    .setStatus(gameRoom.getRoomStatus())
                    .setCreatorId(gameRoom.getCreatorId())
                    .addAllPlayers(playerDataList).build();
            // 推送加入房间信息给房间内所有玩家
            GameRoomClientProto.PlayerInfo roomPlayer = playerDataList.stream().filter(user -> ObjUtil.equals(user.getPlayerId(), playerId)).findFirst().orElseThrow();
            for (GameRoomPlayer gameRoomPlayer : gameRoom.getCurPlayers()) {
                if (ObjUtil.equals(gameRoomPlayer.getId(), playerId)) {
                    continue;
                }
                onlineSessionManager.pushToPlayer(gameRoomPlayer.getId(), GameMessage.success(GameMsgType.PLAYER_JOIN_ROOM, roomPlayer.toByteString()));
            }
            return GameMessage.success(inbound, gameRoomDetail.toByteString());
        });
    }

    public CompletableFuture<GameMessage> leaveGameRoom(GameMessage inbound) {
        GameRoomClientProto.LeaveRequest req = ProtoBufUtil.parseBytes(inbound.getBody(), GameRoomClientProto.LeaveRequest.parser());
        long roomId = req.getRoomId();
        return inRoomAsync(roomId, () -> {
            long playerId = req.getPlayerId();
            GameRoom gameRoom = getGameRoom(roomId);
            if (!gameRoom.containsPlayer(playerId)) {
                throw new BusinessException(ErrorCode.GAME_ROOM_PLAYER_NOT_EXIST);
            }
            gameRoom.removePlayer(playerId);
            // 如果是房主退出，则删除房间
            if (ObjUtil.equals(playerId, gameRoom.getCreatorId())) {
                gameRoomMap.remove(roomId);
            }
            // 推送离开房间信息给房间内所有玩家
            CommonProto.BaseResponse baseResponse = userService.getUser(UserProto.GetUserRequest.newBuilder().setPlayerId(playerId).build());
            if (baseResponse.getCode() != ErrorCode.OK.getCode()) {
                throw new BusinessException(ErrorCode.of(baseResponse.getCode(), ErrorCode.SERVICE_UNAVAILABLE));
            }
            UserProto.UserInfo user = ProtoBufUtil.parseRespBody(baseResponse, UserProto.GetUserResponse.parser()).getUser();
            GameRoomClientProto.PlayerInfo roomPlayer = GameRoomClientProto.PlayerInfo.newBuilder()
                    .setPlayerId(user.getPlayerId()).setNickName(user.getNickname()).build();
            for (GameRoomPlayer curPlayer : gameRoom.getCurPlayers()) {
                onlineSessionManager.pushToPlayer(curPlayer.getId(), GameMessage.success(GameMsgType.PLAYER_LEAVE_ROOM, roomPlayer.toByteString()));
            }
            return GameMessage.success(inbound, ByteString.EMPTY);
        });
    }

    public CompletableFuture<GameMessage> ready(GameMessage inbound) {
        long start = System.currentTimeMillis();
        GameRoomClientProto.ReadyRequest req = ProtoBufUtil.parseBytes(inbound.getBody(), GameRoomClientProto.ReadyRequest.parser());
        long roomId = req.getRoomId();
        return inRoomAsync(roomId, () -> {
            long playerId = req.getPlayerId();
            GameRoom gameRoom = getGameRoom(roomId);
            if (!gameRoom.containsPlayer(playerId)) {
                throw new BusinessException(ErrorCode.GAME_ROOM_PLAYER_NOT_EXIST);
            }
            if (gameRoom.getRoomStatus() != GameRoomClientProto.RoomStatus.WAITING) {
                throw new BusinessException(ErrorCode.GAME_ROOM_STATUS_ERROR);
            }
            gameRoom.updatePlayerStatus(playerId, GameRoomClientProto.UserStatus.READY);
            // 推送准备信息给房间内所有玩家
            CommonProto.BaseResponse baseResponse = userService.getUser(UserProto.GetUserRequest.newBuilder().setPlayerId(playerId).build());
            UserProto.UserInfo user = ProtoBufUtil.parseRespBody(baseResponse, UserProto.GetUserResponse.parser()).getUser();
            GameRoomClientProto.PlayerInfo roomPlayer = GameRoomClientProto.PlayerInfo.newBuilder()
                    .setPlayerId(user.getPlayerId()).setNickName(user.getNickname()).setStatus(GameRoomClientProto.UserStatus.READY).build();
            for (GameRoomPlayer curPlayer : gameRoom.getCurPlayers()) {
                if (ObjUtil.equals(curPlayer.getId(), playerId)) {
                    continue;
                }
                onlineSessionManager.pushToPlayer(curPlayer.getId(), GameMessage.success(GameMsgType.PLAYER_READY, roomPlayer.toByteString()));
            }
            long end = System.currentTimeMillis();
            log.debug("roomId {}, userId {}, ready used time: {}", roomId, user.getPlayerId(), end - start);
            return GameMessage.success(inbound, ByteString.EMPTY);
        });
    }

    public CompletableFuture<GameMessage> startMatchGame(GameMessage inbound) {
        GameRoomClientProto.StartRequest req = ProtoBufUtil.parseBytes(inbound.getBody(), GameRoomClientProto.StartRequest.parser());
        long roomId = req.getRoomId();
        CompletableFuture<GameMessage> completableFuture = inRoomAsync(roomId, () -> {
            long playerId = req.getPlayerId();
            GameRoom gameRoom = getGameRoom(roomId);
            if (!gameRoom.containsPlayer(playerId) || gameRoom.getCreatorId() != playerId) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "Player not in room or not the creator");
            }
            if (gameRoom.getRoomStatus() != GameRoomClientProto.RoomStatus.WAITING) {
                throw new BusinessException(ErrorCode.GAME_ROOM_STATUS_ERROR);
            }
            if (!gameRoom.isALLReady()) {
                throw new BusinessException(ErrorCode.GAME_ROOM_NOT_ALL_READY);
            }
            if (gameRoom.getCurPlayers().size() < 2) {
                throw new BusinessException(ErrorCode.GAME_ROOM_NOT_ENOUGH_PLAYER);
            }
            // 创建一个对战
            GameMatch gameMatch = createGameMatch(gameRoom);
            // 设置房间状态为启动中
            gameRoom.setRoomStatus(GameRoomClientProto.RoomStatus.STARTING);
            gameRoom.setGameMatch(gameMatch);
            // 推送开始游戏消息给房间内所有玩家
            for (GameRoomPlayer curPlayer : gameRoom.getCurPlayers()) {
                // 分配出生点
                gameMatch.distributeSpawnPoint(curPlayer.getId());
                GameRoomClientProto.StartNotice startNotice = GameRoomClientProto.StartNotice.newBuilder()
                        .setRoomId(roomId)
                        .addAllMapData(gameMatch.getMapData())
                        .build();
                onlineSessionManager.pushToPlayer(curPlayer.getId(), GameMessage.success(GameMsgType.GAME_STARTED, startNotice.toByteString()));
            }
            return GameMessage.success(inbound, ByteString.EMPTY);
        });
        completableFuture.whenComplete((outbound, throwable) -> {
            if (throwable != null) {
                return;
            }
           scheduledExecutor.schedule(() -> {
               GameRoom gameRoom = gameRoomMap.get(roomId);
               // 五秒后检查房间内的用户是否收到了startNotice，并向tcpserver发送了ack
               List<GameRoomPlayer> curPlayers = gameRoomMap.get(roomId).getCurPlayers();
               for (GameRoomPlayer curPlayer : curPlayers) {
                   // 房主不需要准备
                   if (!Objects.equals(curPlayer.getId(), gameRoom.getCreatorId()) && curPlayer.getStatus() != GameRoomClientProto.UserStatus.LOADED) {
                       log.warn("Player {} not loaded", curPlayer.getId());
                       // 没有响应ack的玩家踢出房间
                       GameMatch gameMatch = gameRoom.getGameMatch();
                       PlayerInMatch playerInMatch = gameMatch.getPlayers().stream().filter(player -> player.getPlayerId().equals(curPlayer.getId())).findFirst().orElse(null);
                       if (playerInMatch != null) {
                           playerInMatch.setOnline(false);
                       }
                   }
               }
               // 启动游戏
               startMatchGame(gameRoom);
               gameRoom.setRoomStatus(GameRoomClientProto.RoomStatus.PLAYING);
           }, 5, TimeUnit.SECONDS);
        });
        return completableFuture;
    }

    public CompletableFuture<GameMessage> loadedAck(GameMessage inbound) {
        GameRoomClientProto.LoadedAck loadedAck = ProtoBufUtil.parseBytes(inbound.getBody(), GameRoomClientProto.LoadedAck.parser());
        long roomId = loadedAck.getRoomId();
        return inRoomAsync(roomId, () -> {
            long playerId = loadedAck.getPlayerId();
            GameRoom gameRoom = getGameRoom(roomId);
            if (!gameRoom.containsPlayer(playerId)) {
                throw new BusinessException(ErrorCode.GAME_ROOM_PLAYER_NOT_EXIST);
            }
            gameRoom.updatePlayerStatus(playerId, GameRoomClientProto.UserStatus.LOADED);
            return GameMessage.success(inbound, ByteString.EMPTY);
        });
    }

    /**
     * 给 GameMatch 结束后的回调
     *
     * @param roomId 房间id
     */
    public void onGameMatchEnd(Long roomId) {
        GameRoom gameRoom = getGameRoom(roomId);
        gameRoom.setRoomStatus(GameRoomClientProto.RoomStatus.CALCULATING);
        scheduledExecutor.schedule(() -> {
            // 5s 后删除房间
            gameRoomMap.remove(roomId);
        }, 5, TimeUnit.SECONDS);

    }

    /************  Match对局相关 ************/

    private GameMatch createGameMatch(GameRoom room) {
        if (room.getRoomStatus() != GameRoomClientProto.RoomStatus.WAITING) {
            throw new BusinessException(ErrorCode.GAME_ROOM_STATUS_ERROR);
        }
        return new GameMatch(room.getRoomId(), mapGenerateHandler.generateMap(room.getMaxPlayer())
                , room.getMaxPlayer(), 60 * 10, room.getCurPlayers().stream().map(PlayerInMatch::new).toList()
                , this::asyncPushTickToClients, this::onGameMatchEnd);
    }

    public void startMatchGame(GameRoom gameRoom) {
        GameMatch gameMatch = gameRoom.getGameMatch();
        if (gameMatch == null) {
            throw new BusinessException(ErrorCode.GAME_MATCH_NOT_FOUND);
        }
        gameMatch.setStatus(MatchStatus.RUNNING);
        gameMatch.setStartTime(System.currentTimeMillis());
        // 初始化 tick
        gameMatch.initTick();
        //  启动周期性 tick 任务
        ScheduledFuture<?> future = scheduledExecutor.scheduleAtFixedRate(
                gameRoom::asyncTick,
                0, // - 第二个参数：初始延迟（0ms，创建后立即执行第一次tick）
                50,          // - 第三个参数：周期时间（ms，每次tick间隔）
                TimeUnit.MILLISECONDS
        );
        gameMatch.setTickTask(future);
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
    }



    public CompletableFuture<GameMessage> handlerShoot(GameMessage inbound) {
        MatchClientProto.OpRequest request = ProtoBufUtil.parseBytes(inbound.getBody(), MatchClientProto.OpRequest.parser());
        return inRoomAsync(request.getRoomId(), () -> {
            GameMatch gameMatch = getGameMatch(request.getRoomId(), true);
            gameMatch.offerOperation(GameMsgType.TANK_SHOOT, request.getPlayerId(), request.getOpParams());
            return GameMessage.success(inbound, ByteString.EMPTY);
        });
    }

    public CompletableFuture<GameMessage> handlerMove(GameMessage inbound) {
        MatchClientProto.OpRequest request = ProtoBufUtil.parseBytes(inbound.getBody(), MatchClientProto.OpRequest.parser());
        return inRoomAsync(request.getRoomId(), () -> {
            GameMatch gameMatch = getGameMatch(request.getRoomId(), true);
            gameMatch.offerOperation(GameMsgType.TANK_MOVE, request.getPlayerId(), request.getOpParams());
            return GameMessage.success(inbound, ByteString.EMPTY);
        });
    }

    public CompletableFuture<GameMessage> handlerLeaveGame(GameMessage inbound) {
        MatchClientProto.LeaveMatchReq request = ProtoBufUtil.parseBytes(inbound.getBody(), MatchClientProto.LeaveMatchReq.parser());
        return inRoomAsync(request.getRoomId(), () -> {
            GameMatch gameMatch = getGameMatch(request.getRoomId(), false);
            List<PlayerInMatch> players = gameMatch.getPlayers();
            players.stream().filter(playerInMatch -> playerInMatch.getPlayerId().equals(request.getPlayerId())).findFirst().ifPresent(playerInMatch -> {
                playerInMatch.setOnline(false);
            });
            return GameMessage.success(inbound, ByteString.EMPTY);
        });
    }

    private @NonNull GameMatch getGameMatch(Long roomId, boolean checkRunning) {
        if (roomId == null) {
            throw new BusinessException(ErrorCode.GAME_MATCH_NOT_FOUND);
        }
        GameRoom gameRoom = gameRoomMap.get(roomId);
        if (gameRoom == null) {
            throw new BusinessException(ErrorCode.GAME_ROOM_NOT_FOUND);
        }
        GameMatch gameMatch = gameRoom.getGameMatch();
        if (gameMatch == null) {
            throw new BusinessException(ErrorCode.GAME_MATCH_NOT_FOUND);
        }
        if (checkRunning && gameMatch.getStatus() != MatchStatus.RUNNING) {
            throw new BusinessException(ErrorCode.GAME_MATCH_STATUS_ERROR);
        }
        return gameMatch;
    }
}
