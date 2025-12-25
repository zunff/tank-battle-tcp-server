package com.zunf.tankbattletcpserver.entity;

import com.zunf.tankbattletcpserver.executor.SerialExecutor;
import com.zunf.tankbattletcpserver.grpc.game.room.GameRoomClientProto;
import lombok.Data;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
public class GameRoom {

    private Long roomId;
    private String roomName;
    private Integer maxPlayer;

    private List<Long> curPlayerIds;
    private Long creatorId;

    private GameRoomClientProto.RoomStatus roomStatus;

    /**
     * 房间内指令串行执行器
     */
    private SerialExecutor serialExecutor;

    public GameRoom(Long roomId, Long creatorId, String roomName, Integer maxPlayer, SerialExecutor serialExecutor) {
        this.roomId = roomId;
        this.creatorId = creatorId;
        this.roomName = roomName;
        this.maxPlayer = maxPlayer;
        this.curPlayerIds = new CopyOnWriteArrayList<>();
        this.roomStatus = GameRoomClientProto.RoomStatus.WAITING;
        this.serialExecutor = serialExecutor;
    }
}
