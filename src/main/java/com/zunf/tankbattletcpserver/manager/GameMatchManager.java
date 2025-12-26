package com.zunf.tankbattletcpserver.manager;

import com.zunf.tankbattletcpserver.common.BusinessException;
import com.zunf.tankbattletcpserver.entity.GameMatch;
import com.zunf.tankbattletcpserver.entity.GameRoom;
import com.zunf.tankbattletcpserver.enums.ErrorCode;
import com.zunf.tankbattletcpserver.grpc.game.room.GameRoomClientProto;
import com.zunf.tankbattletcpserver.handler.MapGenerateHandler;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class GameMatchManager {

    @Resource
    private MapGenerateHandler mapGenerateHandler;

    AtomicLong atomicInteger = new AtomicLong(0);

    private final Map<Long, GameMatch> gameMatchMap = new ConcurrentHashMap<>();

    public Long generateMatchId() {
        return atomicInteger.incrementAndGet();
    }

    public GameMatch createGameMatch(GameRoom room) {
        if (room.getRoomStatus() != GameRoomClientProto.RoomStatus.WAITING) {
            throw new BusinessException(ErrorCode.GAME_ROOM_STATUS_ERROR);
        }
        Long matchId = generateMatchId();
        GameMatch gameMatch = new GameMatch(matchId, room.getRoomId(), mapGenerateHandler.generateMap(room.getMaxPlayer()), room.getMaxPlayer(), 60 * 10);
        gameMatchMap.put(matchId, gameMatch);
        return gameMatch;
    }
}
