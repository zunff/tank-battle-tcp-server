package com.zunf.tankbattletcpserver.entity;

import com.google.protobuf.ByteString;
import com.zunf.tankbattletcpserver.constant.ProtocolConstant;
import com.zunf.tankbattletcpserver.enums.ErrorCode;
import com.zunf.tankbattletcpserver.enums.GameMsgType;
import com.zunf.tankbattletcpserver.util.ProtoBufUtil;
import lombok.Data;

@Data
public class GameMessage {

    private GameMsgType msgType;
    private byte version;
    private int requestId;
    private int bodyLength;
    /**
     * 原始 body 字节，业务层可以再反序列化成 DTO
     */
    private byte[] body;

    /**
     * 可选：附加一些网关内部字段（不参与网络编码）
     */
    private long playerId;
    private String traceId;

    public GameMessage(GameMsgType msgType, byte version, int requestId, byte[] body) {
        this.msgType = msgType;
        this.version = version;
        this.requestId = requestId;
        this.body = body;
        this.bodyLength = (body != null ? body.length : 0);
    }

    /**
     * 构建成功响应 body 会包一层 BaseResponse
     */
    public static GameMessage success(GameMessage inbound, ByteString body) {
        return new GameMessage(inbound.getMsgType(), ProtocolConstant.PROTOCOL_VERSION, inbound.getRequestId(),  ProtoBufUtil.successResp(body));
    }

    public static GameMessage success(GameMsgType msgType, ByteString body) {
        return new GameMessage(msgType, ProtocolConstant.PROTOCOL_VERSION, 0,  ProtoBufUtil.successResp(body));
    }

    public static GameMessage fail(GameMessage inbound, ErrorCode errorCode) {
        return new GameMessage(inbound.getMsgType(), ProtocolConstant.PROTOCOL_VERSION, inbound.getRequestId(), ProtoBufUtil.failResp(errorCode));
    }
}
