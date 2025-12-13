package com.zunf.tankbattletcpserver.entity;

import com.zunf.tankbattletcpserver.enums.GameMsgType;
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
}
