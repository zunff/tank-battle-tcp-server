package com.zunf.tankbattletcpserver.model.entity.game;

import com.zunf.tankbattletcpserver.executor.SerialExecutor;
import com.zunf.tankbattletcpserver.grpc.game.room.GameRoomClientProto;
import lombok.Data;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
public class GameRoom {

    private Long roomId;
    private String roomName;
    private Integer maxPlayer;

    private List<GameRoomPlayer> curPlayers;
    private Long creatorId;

    private GameRoomClientProto.RoomStatus roomStatus;

    private GameMatch gameMatch;

    /**
     * 房间内指令串行执行器
     */
    private SerialExecutor serialExecutor;

    public GameRoom(Long roomId, Long creatorId, String roomName, Integer maxPlayer, SerialExecutor serialExecutor) {
        this.roomId = roomId;
        this.creatorId = creatorId;
        this.roomName = roomName;
        this.maxPlayer = maxPlayer;
        this.curPlayers = new CopyOnWriteArrayList<>();
        this.roomStatus = GameRoomClientProto.RoomStatus.WAITING;
        this.serialExecutor = serialExecutor;
    }

    public void addPlayer(Long playerId, String nickname) {
        curPlayers.add(new GameRoomPlayer(playerId, nickname, GameRoomClientProto.UserStatus.LOBBY));
    }

    public void removePlayer(Long playerId) {
        curPlayers.removeIf(player -> player.getId().equals(playerId));
    }

    public boolean containsPlayer(Long playerId) {
        return curPlayers.stream().anyMatch(player -> player.getId().equals(playerId));
    }

    public void updatePlayerStatus(Long playerId, GameRoomClientProto.UserStatus status) {
        curPlayers.stream()
                .filter(player -> player.getId().equals(playerId))
                .findFirst()
                .ifPresent(player -> player.setStatus(status));
    }

    public boolean isFull() {
        return curPlayers.size() >= maxPlayer;
    }

    public boolean isALLReady() {
        return curPlayers.stream().filter(player -> !Objects.equals(player.getId(), creatorId))
                .allMatch(player -> player.getStatus() == GameRoomClientProto.UserStatus.READY);
    }

    public void asyncTick() {
        serialExecutor.submit(() -> {
            if (gameMatch != null) {
                gameMatch.tick();
            }
            return null;
        });
    }
}
