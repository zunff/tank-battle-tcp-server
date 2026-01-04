package com.zunf.tankbattletcpserver.model.entity.game;

import com.zunf.tankbattletcpserver.grpc.game.room.GameRoomClientProto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameRoomPlayer {

    private Long id;

    private GameRoomClientProto.UserStatus status;
}
