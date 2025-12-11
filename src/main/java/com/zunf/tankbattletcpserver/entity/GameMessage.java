package com.zunf.tankbattletcpserver.entity;

import com.zunf.tankbattletcpserver.enums.GameMsgType;
import lombok.Data;

@Data
public class GameMessage {

    private GameMsgType msgType;
    private byte version;
    private int bodyLength;
    private byte[] body; // 原始 body 字节，业务层可以再反序列化成 DTO

    // 可选：附加一些网关内部字段（不参与网络编码）
    private long playerId;   // SessionHandler 填
    private String traceId;  // 可选链路追踪

    public GameMessage(GameMsgType msgType, byte version, byte[] body) {
        this.msgType = msgType;
        this.version = version;
        this.body = body;
        this.bodyLength = (body != null ? body.length : 0);
    }
}
