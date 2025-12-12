package com.zunf.tankbattletcpserver.handler.message;

import com.example.game.proto.GameProto;
import com.google.protobuf.InvalidProtocolBufferException;
import com.zunf.tankbattletcpserver.entity.GameMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoginMessageHandler {

    public Long handle(GameMessage message) {
        GameProto.LoginRequest req;
        try {
            req = GameProto.LoginRequest.parseFrom(message.getBody());
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        String token = req.getToken();
        log.info("token: {}", token);
        // 模拟鉴权
        return 12345L;
    }
}
