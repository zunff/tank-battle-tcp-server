package com.zunf.tankbattletcpserver.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum MatchStatus {

    WAITING(0),    // 房间已准备，未开始
    RUNNING(1),    // 进行中
    FINISHED(2),   // 正常结束
    CANCELED(3);   // 中途取消/解散

    private int code;


    public static MatchStatus of(int code) {
        for (MatchStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown MatchStatus code: " + code);
    }
}
