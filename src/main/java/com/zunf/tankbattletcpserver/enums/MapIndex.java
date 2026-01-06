package com.zunf.tankbattletcpserver.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum MapIndex {

    /**
     * 空地 0、可破坏墙 1、不可破坏墙 2、出生点 3、已破坏墙 4
     */
    EMPTY((byte) 0),
    WALL((byte) 1),
    BRICK((byte) 2),
    SPAWN((byte) 3),
    DESTROYED_WALL((byte) 4);



    private byte code;

    public static MapIndex of(byte code) {
        return Arrays.stream(MapIndex.values()).filter(mapIndex -> mapIndex.code == code).findFirst().orElseThrow();
    }

    public boolean isWall() {
        return this == WALL || this == BRICK;
    }
}
