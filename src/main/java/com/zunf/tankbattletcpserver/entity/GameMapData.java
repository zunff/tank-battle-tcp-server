package com.zunf.tankbattletcpserver.entity;

import lombok.Data;

import java.util.List;

@Data
public class GameMapData {

    byte[][] mapData;

    List<int[]> spawnPoints;
}
