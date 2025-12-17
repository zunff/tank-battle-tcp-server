package com.zunf.tankbattletcpserver.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum GameMsgType {
    ERROR(0),
    LOGIN(1),
    LOGOUT(2),
    CREATE_ROOM(3),
    UNKNOWN(255);

    private final int code;

    public static GameMsgType of(int code) {
        return Arrays.stream(values()).filter(v -> v.code == code).findFirst().orElse(UNKNOWN);
    }
}
