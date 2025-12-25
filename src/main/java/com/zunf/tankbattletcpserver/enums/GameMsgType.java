package com.zunf.tankbattletcpserver.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum GameMsgType {
    // client -> server
    PING(1),
    LOGIN(2),
    LOGOUT(3),
    CREATE_ROOM(4),
    PAGE_ROOM(5),
    JOIN_ROOM(6),
    LEAVE_ROOM(7),
    READY(8),

    // server -> client
    PONG(10001),
    PLAYER_JOIN_ROOM(10002),
    PLAYER_LEAVE_ROOM(10003),
    PLAYER_READY(10004),

    // common
    ERROR(0),
    UNKNOWN(20001);

    private final int code;

    public static GameMsgType of(int code) {
        return Arrays.stream(values()).filter(v -> v.code == code).findFirst().orElse(UNKNOWN);
    }
}
