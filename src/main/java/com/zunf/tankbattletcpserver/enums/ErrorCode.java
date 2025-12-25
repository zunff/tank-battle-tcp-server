package com.zunf.tankbattletcpserver.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 0: success
    OK(0, "OK"),

    // 1xxx: common
    INTERNAL_ERROR(1000, "Internal error"),
    UNKNOWN_ERROR(1001, "Unknown error"),
    NOT_IMPLEMENTED(1002, "Not implemented"),

    // 2xxx: protocol / params
    BAD_REQUEST(2000, "Bad request"),
    INVALID_ARGUMENT(2001, "Invalid argument"),
    MISSING_ARGUMENT(2002, "Missing argument"),
    BAD_PROTOCOL_VERSION(2003, "Bad protocol version"),
    UNSUPPORTED_COMMAND(2004, "Unsupported command"),
    PAYLOAD_TOO_LARGE(2005, "Payload too large"),
    PROTO_PARSE_ERROR(2006, "Proto parse error"),

    // 3xxx: auth / permission
    UNAUTHORIZED(3001, "Unauthorized"),
    TOKEN_EXPIRED(3002, "Token expired"),
    FORBIDDEN(3003, "Forbidden"),

    // 4xxx: resource
    NOT_FOUND(4001, "Not found"),
    ALREADY_EXISTS(4002, "Already exists"),
    CONFLICT(4003, "Conflict"),

    // 5xxx: retryable / transient
    TIMEOUT(5001, "Timeout"),
    RATE_LIMITED(5002, "Rate limited"),
    SERVICE_UNAVAILABLE(5003, "Service unavailable"),

    // 6xxx: business error
    GAME_ROOM_NOT_FOUND(6001, "Room not found"),
    GAME_ROOM_FULL(6002, "Room full"),
    GAME_ROOM_PLAYER_EXIST(6003, "Player exist"),
    GAME_ROOM_PLAYER_NOT_EXIST(6004, "Player not exist"),
    GAME_ROOM_NOT_CREATOR(6005, "Not creator"),
    GAME_ROOM_ALREADY_START(6006, "Already start"),
    ;

    private final int code;
    private final String msg;

    public boolean isOk() {
        return this.code == 0;
    }

    public boolean isFail() {
        return this.code != 0;
    }

    public static ErrorCode of(int code) {
        for (ErrorCode e : values()) {
            if (e.code == code) return e;
        }
        return UNKNOWN_ERROR;
    }

    public static ErrorCode of(int code, ErrorCode def) {
        for (ErrorCode e : values()) {
            if (e.code == code) return e;
        }
        return def;
    }
}
