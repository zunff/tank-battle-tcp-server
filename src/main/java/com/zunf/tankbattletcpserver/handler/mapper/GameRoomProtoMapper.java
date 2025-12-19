package com.zunf.tankbattletcpserver.handler.mapper;

import com.zunf.tankbattletcpserver.annotation.IgnoreProtoCommonFields;
import com.zunf.tankbattletcpserver.grpc.CommonProto;
import com.zunf.tankbattletcpserver.grpc.game.room.GameRoomClientProto;
import com.zunf.tankbattletcpserver.grpc.server.room.GameRoomProto;
import com.zunf.tankbattletcpserver.util.ProtoBufUtil;
import org.mapstruct.Mapper;

@Mapper(config = CommonProtoMapperConfig.class)
public interface GameRoomProtoMapper {

    @IgnoreProtoCommonFields
    GameRoomProto.CreateGameRoomRequest toCreateRoomReq(GameRoomClientProto.CreateGameRoomRequest clientReq);

    default byte[] toCreateRoomResp(CommonProto.BaseResponse baseResp) {
        return ProtoBufUtil.convertAndBuildResp(
                baseResp,
                // 1. 指定Server侧解析器
                GameRoomProto.CreateGameRoomResponse.parser(),
                // 2. 指定Client侧Builder
                GameRoomClientProto.CreateGameRoomResponse.newBuilder(),
                // 3. 指定业务字段映射规则（无反射，类型安全）
                (serverResp, clientBuilder) -> {
                    clientBuilder.setRoomId(serverResp.getRoomId());
                }
        );
    }

    @IgnoreProtoCommonFields
    GameRoomProto.PageRequest toPageRoomReq(GameRoomClientProto.PageRequest clientReq);
    @IgnoreProtoCommonFields
    GameRoomClientProto.GameRoomData toGameRoomDataResp(GameRoomProto.GameRoomData serverData);
    default byte[] toPageRoomResp(CommonProto.BaseResponse baseResp) {
        return ProtoBufUtil.convertAndBuildResp(
                baseResp,
                // 1. 指定Server侧解析器
                GameRoomProto.PageResponse.parser(),
                // 2. 指定Client侧Builder
                GameRoomClientProto.PageResponse.newBuilder(),
                // 3. 指定业务字段映射规则（无反射，类型安全）
                (serverResp, clientBuilder) -> {
                    clientBuilder.addAllData(serverResp.getDataList().stream().map(this::toGameRoomDataResp).toList());
                    clientBuilder.setTotal(serverResp.getTotal());
                }
        );
    }

    @IgnoreProtoCommonFields
    GameRoomProto.JoinGameRoomRequest toJoinRoomReq(GameRoomClientProto.JoinGameRoomRequest clientReq);
    @IgnoreProtoCommonFields
    GameRoomClientProto.GameRoomPlayerData toGameRoomPlayerDataResp(GameRoomProto.GameRoomPlayerData serverData);
    default byte[] toJoinRoomResp(CommonProto.BaseResponse baseResp) {
        return ProtoBufUtil.convertAndBuildResp(
                baseResp,
                // 1. 指定Server侧解析器
                GameRoomProto.GameRoomDetail.parser(),
                // 2. 指定Client侧Builder
                GameRoomClientProto.GameRoomDetail.newBuilder(),
                // 3. 指定业务字段映射规则（无反射，类型安全）
                (serverResp, clientBuilder) -> {
                    clientBuilder.setId(serverResp.getId());
                    clientBuilder.setName(serverResp.getName());
                    clientBuilder.setMaxPlayers(serverResp.getMaxPlayers());
                    clientBuilder.setCreatorId(serverResp.getCreatorId());
                    clientBuilder.setStatus(GameRoomClientProto.RoomStatus.forNumber(serverResp.getStatus().getNumber()));
                    clientBuilder.addAllPlayers(serverResp.getPlayersList().stream().map(this::toGameRoomPlayerDataResp).toList());
                }
        );
    }


    @IgnoreProtoCommonFields
    GameRoomProto.LeaveGameRoomRequest toLeaveRoomReq(GameRoomClientProto.LeaveGameRoomRequest clientReq);
    default byte[] toLeaveRoomResp(CommonProto.BaseResponse baseResp) {
        return ProtoBufUtil.convertAndBuildResp(
                baseResp, null, null, null
        );
    }
}
