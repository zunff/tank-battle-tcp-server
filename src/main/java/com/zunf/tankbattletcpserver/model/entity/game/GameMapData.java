package com.zunf.tankbattletcpserver.model.entity.game;

import lombok.Data;

import java.util.List;

@Data
public class GameMapData {

    private byte[][] mapData;

    private List<int[]> spawnPoints;
}
