package com.zunf.tankbattletcpserver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "game.map")
public class MapConfig {

    /**
     * 地图宽度（格子数）
     */
    private int width = 32;

    /**
     * 地图高度（格子数）
     */
    private int height = 32;

    /**
     * 砖块占比（0~1），比如 0.18 表示 18%
     */
    private double brickRate = 0.18;

    /**
     * 不可破坏墙占比（0~1）
     */
    private double wallRate = 0.05;

    /**
     * 出生点距离边界的最小间距（格子数）
     */
    private int spawnMargin = 2;

    // 地图元素编码
    private byte empty = 0;   // 空地
    private byte wall = 1;    // 不可破坏墙
    private byte brick = 2;   // 可破坏砖块
    private byte spawn = 3;   // 出生点
}
