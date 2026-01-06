package com.zunf.tankbattletcpserver.constant;

public interface MapConstant {

    /**
     * 格子尺寸固定为32px
     */
    int GRID_SIZE = 32;

    /**
     * 坦克基础尺寸比例
     */
    double TANK_BASE_RATIO =  0.8;
    /**
     * 坦克长宽
     */
    double TANK_LONG_SIDE = GRID_SIZE * TANK_BASE_RATIO * 1.2;
    double TANK_SHORT_SIDE = GRID_SIZE * TANK_BASE_RATIO * 0.85;


    /**
     * 地图宽度（格子数）
     */
     int WIDTH = 32;

    /**
     * 地图高度（格子数）
     */
     int HEIGHT = 32;

     int MAP_PX = WIDTH * GRID_SIZE;
     int MAP_PY = HEIGHT * GRID_SIZE;

    /**
     * 砖块占比（0~1），比如 0.18 表示 18%
     */
     double BRICK_RATE = 0.18;

    /**
     * 不可破坏墙占比（0~1）
     */
     double WALL_RATE = 0.05;

    /**
     * 出生点距离边界的最小间距（格子数）
     */
     int SPAWN_MARGIN = 2;
}
