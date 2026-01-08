package com.zunf.tankbattletcpserver.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum MatchEndReason {

    NORMAL(0),     // 正常打完（比如有人达到目标击杀数）
    TIMEOUT(1),    // 时间到
    ALL_LEFT(2),   // 所有人退出
    ERROR(3),      // 异常终止
    DRAW(4),      // 平局
    ;
    private int code;

    // 可选：通过 code 反查枚举
    public static MatchEndReason of (int code) {
        for (MatchEndReason reason : values()) {
            if (reason.code == code) {
                return reason;
            }
        }
        throw new IllegalArgumentException("Unknown MatchEndReason code: " + code);
    }
}
