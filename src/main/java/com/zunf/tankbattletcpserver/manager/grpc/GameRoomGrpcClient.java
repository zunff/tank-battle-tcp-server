package com.zunf.tankbattletcpserver.manager.grpc;

import cn.hutool.core.bean.BeanUtil;
import com.zunf.tankbattletcpserver.entity.GameMessage;
import com.zunf.tankbattletcpserver.enums.ErrorCode;
import com.zunf.tankbattletcpserver.grpc.CommonProto;
import com.zunf.tankbattletcpserver.grpc.game.room.GameRoomClientProto;
import com.zunf.tankbattletcpserver.grpc.server.room.GameRoomProto;
import com.zunf.tankbattletcpserver.grpc.server.room.GameRoomServiceGrpc;
import com.zunf.tankbattletcpserver.util.ProtoBufUtil;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GameRoomGrpcClient extends GameRoomServiceGrpc.GameRoomServiceImplBase {

    @GrpcClient("tank-battle-backend")
    private GameRoomServiceGrpc.GameRoomServiceBlockingStub gameRoomService;

    public GameMessage createGameRoom(GameMessage inbound) {
        // 解析
        GameRoomClientProto.CreateGameRoomRequest request = ProtoBufUtil.parseBytes(inbound.getBody(), GameRoomClientProto.CreateGameRoomRequest.parser());
        if (request == null) {
            return GameMessage.fail(inbound, ErrorCode.INVALID_ARGUMENT);
        }
        // 调用
        CommonProto.BaseResponse baseResp = gameRoomService.createGameRoom(BeanUtil.copyProperties(request, GameRoomProto.CreateGameRoomRequest.class));
        if (baseResp == null) {
            return GameMessage.fail(inbound, ErrorCode.UNKNOWN_ERROR);
        }
        if (baseResp.getCode() != 0) {
            return GameMessage.fail(inbound, ErrorCode.of(baseResp.getCode()));
        }
        // 封装后返回
        GameRoomClientProto.CreateGameRoomResponse createGameRoomResponse = ProtoBufUtil.parseRespBody(baseResp, GameRoomClientProto.CreateGameRoomResponse.parser());
        GameRoomClientProto.CreateGameRoomResponse resp = BeanUtil.copyProperties(createGameRoomResponse, GameRoomClientProto.CreateGameRoomResponse.class);
        return GameMessage.success(inbound, ProtoBufUtil.successResp(resp.toByteString()));
    }
}
