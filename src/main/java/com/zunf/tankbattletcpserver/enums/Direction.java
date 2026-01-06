package com.zunf.tankbattletcpserver.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum Direction {
    UP(0), // 上
    DOWN(1), // 下
    LEFT(2), // 左
    RIGHT(3); // 右

    private final int code;

    public static Direction random() {
        return values()[(int) (Math.random() * values().length)];
    }

    public static Direction of(int code) {
        return Arrays.stream(values()).filter(value -> value.code == code).findFirst().orElseThrow();
    }
}