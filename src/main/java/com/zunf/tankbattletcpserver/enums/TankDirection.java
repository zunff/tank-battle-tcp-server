package com.zunf.tankbattletcpserver.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
public enum TankDirection {
    UP(0), // 上
    DOWN(1), // 下
    LEFT(2), // 左
    RIGHT(3); // 右

    private final int code;

    public static TankDirection random() {
        return values()[(int) (Math.random() * values().length)];
    }
}