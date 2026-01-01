package com.zunf.tankbattletcpserver.manager;

import cn.hutool.core.util.ObjUtil;
import com.google.protobuf.ByteString;
import com.zunf.tankbattletcpserver.common.BusinessException;
import com.zunf.tankbattletcpserver.entity.GameMatch;
import com.zunf.tankbattletcpserver.entity.GameMessage;
import com.zunf.tankbattletcpserver.entity.GameRoom;
import com.zunf.tankbattletcpserver.entity.GameRoomPlayer;
import com.zunf.tankbattletcpserver.enums.ErrorCode;
import com.zunf.tankbattletcpserver.enums.GameMsgType;
import com.zunf.tankbattletcpserver.executor.SerialExecutor;
import com.zunf.tankbattletcpserver.grpc.CommonProto;
import com.zunf.tankbattletcpserver.grpc.game.room.GameRoomClientProto;
import com.zunf.tankbattletcpserver.grpc.server.user.UserProto;
import com.zunf.tankbattletcpserver.grpc.server.user.UserServiceGrpc;
import com.zunf.tankbattletcpserver.util.ProtoBufUtil;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
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
    private OnlineSessionManager onlineSessionManager;

    @Resource
    private GameMatchManager gameMatchManager;

    @Resource(name = "gameRoomExecutor")
    private ExecutorService gameRoomExecutor;

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
        GameRoom gameRoom = new GameRoom(roomId, creatorId, req.getName(), req.getMaxPlayers(), new SerialExecutor(gameRoomExecutor));
        // 创建者自动加入房间
        gameRoom.addPlayer(creatorId);
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
            gameRoom.addPlayer(playerId);
            // 请求后端查询玩家信息
            List<Long> playerIds = gameRoom.getCurPlayers().stream().map(GameRoomPlayer::getId).toList();
            CommonProto.BaseResponse baseResponse = userService.listUser(UserProto.ListUserRequest.newBuilder().addAllPlayerIds(playerIds).build());
            if (baseResponse.getCode() != ErrorCode.OK.getCode()) {
                throw new BusinessException(ErrorCode.of(baseResponse.getCode(), ErrorCode.SERVICE_UNAVAILABLE));
            }
            List<UserProto.UserInfo> usersList = ProtoBufUtil.parseRespBody(baseResponse, UserProto.ListUserResponse.parser()).getUsersList();
            if (ObjUtil.notEqual(usersList.size(), playerIds.size())) {
                throw new BusinessException(ErrorCode.UNKNOWN_ERROR);
            }

            // 构建房间详情
            List<GameRoomClientProto.PlayerInfo> playerDataList = usersList.stream().map(user -> GameRoomClientProto.PlayerInfo.newBuilder()
                    .setPlayerId(user.getPlayerId()).setNickName(user.getNickname()).build()).toList();
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
            return GameMessage.success(inbound, ByteString.EMPTY);
        });
    }

    public CompletableFuture<GameMessage> startGame(GameMessage inbound) {
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
            // 创建一个对战
            GameMatch gameMatch = gameMatchManager.createGameMatch(gameRoom);
            // 设置房间状态为启动中
            gameRoom.setRoomStatus(GameRoomClientProto.RoomStatus.STARTING);
            gameRoom.setGameMatchId(gameMatch.getMatchId());
            // 推送开始游戏消息给房间内所有玩家
            for (GameRoomPlayer curPlayer : gameRoom.getCurPlayers()) {
                GameRoomClientProto.StartNotice startNotice = GameRoomClientProto.StartNotice.newBuilder()
                        .setRoomId(roomId)
                        .setMatchId(gameMatch.getMatchId())
                        .addAllMapData(gameMatch.getMapData())
                        .setSpawnPoint(gameMatch.getSpawnPoint(playerId))
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
                       // todo 没有响应ack的玩家踢出房间
                   }
               }
               // 启动游戏
               gameMatchManager.startGame(gameRoom.getGameMatchId());
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
}
