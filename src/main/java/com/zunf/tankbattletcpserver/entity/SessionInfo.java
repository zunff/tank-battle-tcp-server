package com.zunf.tankbattletcpserver.entity;

import lombok.Data;

@Data
public class SessionInfo {

    private Long playerId;

    private boolean authenticated;
}
