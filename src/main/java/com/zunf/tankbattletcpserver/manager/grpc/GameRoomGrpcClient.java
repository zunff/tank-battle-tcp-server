package com.zunf.tankbattletcpserver.manager.grpc;

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
        GameRoomProto.CreateGameRoomRequest req = gameRoomProtoMapper.toCreateRoomReq(clientRequest);
        CommonProto.BaseResponse baseResp = gameRoomService.createGameRoom(req);
        // 转换 成最终响应
        byte[] respBytes = gameRoomProtoMapper.toCreateRoomResp(baseResp);
        return GameMessage.success(inbound, respBytes);
    }

    public GameMessage pageGameRoom(GameMessage inbound) {
        // 解析
        GameRoomClientProto.PageRequest clientRequest = ProtoBufUtil.parseBytes(inbound.getBody(), GameRoomClientProto.PageRequest.parser());
        // 调用
        GameRoomProto.PageRequest req = gameRoomProtoMapper.toPageRoomReq(clientRequest);
        CommonProto.BaseResponse baseResp = gameRoomService.pageGameRoom(req);
        // 构建
        byte[] respBytes = gameRoomProtoMapper.toPageRoomResp(baseResp);
        return GameMessage.success(inbound, respBytes);
    }

    public GameMessage joinGameRoom(GameMessage inbound) {
        // 解析
        GameRoomClientProto.JoinGameRoomRequest clientRequest = ProtoBufUtil.parseBytes(inbound.getBody(), GameRoomClientProto.JoinGameRoomRequest.parser());
        // 调用
        GameRoomProto.JoinGameRoomRequest req = gameRoomProtoMapper.toJoinRoomReq(clientRequest);
        CommonProto.BaseResponse baseResp = gameRoomService.joinGameRoom(req);
        // 构建
        byte[] respBytes = gameRoomProtoMapper.toJoinRoomResp(baseResp);
        return GameMessage.success(inbound, respBytes);
    }

    public GameMessage leaveGameRoom(GameMessage inbound) {
        // 解析
        GameRoomClientProto.LeaveGameRoomRequest clientRequest = ProtoBufUtil.parseBytes(inbound.getBody(), GameRoomClientProto.LeaveGameRoomRequest.parser());
        // 调用
        GameRoomProto.LeaveGameRoomRequest req = gameRoomProtoMapper.toLeaveRoomReq(clientRequest);
        CommonProto.BaseResponse baseResp = gameRoomService.leaveGameRoom(req);
        // 构建
        byte[] respBytes = gameRoomProtoMapper.toLeaveRoomResp(baseResp);
        return GameMessage.success(inbound, respBytes);
    }
}
