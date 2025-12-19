package com.zunf.tankbattletcpserver.handler;

import com.zunf.tankbattletcpserver.entity.GameMessage;
import com.zunf.tankbattletcpserver.enums.ErrorCode;
import com.zunf.tankbattletcpserver.enums.GameMsgType;
import com.zunf.tankbattletcpserver.manager.grpc.GameRoomGrpcClient;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class MsgTypeHandlerRegister {

    @Resource
    private GameRoomGrpcClient gameRoomGrpcClient;

    private final Map<GameMsgType, Function<GameMessage, GameMessage>> registry = new HashMap<>();

    @PostConstruct
    public void register() {
        registry.put(GameMsgType.CREATE_ROOM, gameRoomGrpcClient::createGameRoom);
        registry.put(GameMsgType.PAGE_ROOM, gameRoomGrpcClient::pageGameRoom);
        registry.put(GameMsgType.JOIN_ROOM, gameRoomGrpcClient::joinGameRoom);
        registry.put(GameMsgType.LEAVE_ROOM, gameRoomGrpcClient::leaveGameRoom);
    }

    public GameMessage handle(GameMessage inbound) {
        Function<GameMessage, GameMessage> handler = registry.get(inbound.getMsgType());
        if (handler == null) {
            return GameMessage.fail(inbound, ErrorCode.UNSUPPORTED_COMMAND);
        }
        return handler.apply(inbound);
    }

}
