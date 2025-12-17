package com.zunf.tankbattletcpserver.manager.grpc;

import cn.hutool.core.bean.BeanUtil;
import com.zunf.tankbattletcpserver.entity.GameMessage;
import com.zunf.tankbattletcpserver.grpc.CommonProto;
import com.zunf.tankbattletcpserver.grpc.game.room.GameRoomClientProto;
import com.zunf.tankbattletcpserver.grpc.server.room.GameRoomProto;
import com.zunf.tankbattletcpserver.grpc.server.room.GameRoomServiceGrpc;
import com.zunf.tankbattletcpserver.handler.mapper.GameRoomProtoMapper;
import com.zunf.tankbattletcpserver.util.ProtoBufUtil;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class GameRoomGrpcClient extends GameRoomServiceGrpc.GameRoomServiceImplBase {

    @GrpcClient("tank-battle-backend")
    private GameRoomServiceGrpc.GameRoomServiceBlockingStub gameRoomService;

    @Resource
    private GameRoomProtoMapper gameRoomProtoMapper;

    public GameMessage createGameRoom(GameMessage inbound) {
        // 解析
        GameRoomClientProto.CreateGameRoomRequest clientRequest = ProtoBufUtil.parseBytes(inbound.getBody(), GameRoomClientProto.CreateGameRoomRequest.parser());
        // 调用
        GameRoomProto.CreateGameRoomRequest req = gameRoomProtoMapper.toCreateGameRoomReq(clientRequest);
        CommonProto.BaseResponse baseResp = gameRoomService.createGameRoom(req);
        // 转换 成最终响应
        byte[] respBytes = gameRoomProtoMapper.toCreateGameRoomResp(baseResp);
        return GameMessage.success(inbound, respBytes);
    }
}
