package com.zunf.tankbattletcpserver.model.entity;

import lombok.Data;

@Data
public class SessionInfo {

    private Long playerId;

    private boolean authenticated;
}
