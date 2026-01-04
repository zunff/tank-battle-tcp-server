package com.zunf.tankbattletcpserver.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
}
