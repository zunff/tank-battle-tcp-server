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
    GameRoomProto.CreateGameRoomRequest toCreateGameRoomReq(GameRoomClientProto.CreateGameRoomRequest clientReq);

    default byte[] toCreateGameRoomResp(CommonProto.BaseResponse baseResp) {
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
}
