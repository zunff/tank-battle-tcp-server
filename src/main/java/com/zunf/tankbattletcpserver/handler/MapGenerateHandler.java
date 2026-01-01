package com.zunf.tankbattletcpserver.handler;

import com.zunf.tankbattletcpserver.config.MapConfig;
import com.zunf.tankbattletcpserver.entity.GameMapData;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Component
public class MapGenerateHandler {

    @Resource
    private MapConfig mapConfig;

    public GameMapData generateMap(int maxPlayers) {
        GameMapData gameMapData = new GameMapData();

        int width = mapConfig.getWidth();
        int height = mapConfig.getHeight();
        byte[][] map = new byte[height][width];

        // 初始化为空地
        for (int y = 0; y < height; y++) {
            Arrays.fill(map[y], mapConfig.getEmpty());
        }

        // 边界墙
        for (int x = 0; x < width; x++) {
            map[0][x] = mapConfig.getWall();
            map[height - 1][x] = mapConfig.getWall();
        }
        for (int y = 0; y < height; y++) {
            map[y][0] = mapConfig.getWall();
            map[y][width - 1] = mapConfig.getWall();
        }

        // 出生点
        List<int[]> spawnPoints = generateSpawnPoints(width, height, maxPlayers);
        gameMapData.setSpawnPoints(spawnPoints);
        for (int[] p : spawnPoints) {
            int sx = p[0], sy = p[1];
            map[sy][sx] = mapConfig.getSpawn();
            clearAround(map, sx, sy, 1);
        }

        // 随机障碍
        Random random = new Random();
        double brickRate = mapConfig.getBrickRate();
        double wallRate = mapConfig.getWallRate();

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (isNearSpawn(x, y, spawnPoints, 2)) {
                    continue;
                }
                double r = random.nextDouble();
                if (r < wallRate) {
                    map[y][x] = mapConfig.getWall();
                } else if (r < wallRate + brickRate) {
                    map[y][x] = mapConfig.getBrick();
                }
            }
        }
        gameMapData.setMapData(map);
        return gameMapData;
    }

    private List<int[]> generateSpawnPoints(int width, int height, int maxPlayers) {
        List<int[]> points = new ArrayList<>();
        int m = mapConfig.getSpawnMargin();

        List<int[]> candidates = Arrays.asList(
                new int[]{m, m},
                new int[]{width - 1 - m, m},
                new int[]{m, height - 1 - m},
                new int[]{width - 1 - m, height - 1 - m}
        );

        int count = Math.min(Math.max(maxPlayers, 2), 4);
        for (int i = 0; i < count; i++) {
            points.add(candidates.get(i));
        }
        return points;
    }

    private void clearAround(byte[][] map, int cx, int cy, int radius) {
        int h = map.length;
        int w = map[0].length;
        for (int y = Math.max(1, cy - radius); y <= Math.min(h - 2, cy + radius); y++) {
            for (int x = Math.max(1, cx - radius); x <= Math.min(w - 2, cx + radius); x++) {
                map[y][x] = mapConfig.getEmpty();
            }
        }
    }

    private boolean isNearSpawn(int x, int y, List<int[]> spawnPoints, int radius) {
        for (int[] p : spawnPoints) {
            if (Math.abs(p[0] - x) <= radius && Math.abs(p[1] - y) <= radius) {
                return true;
            }
        }
        return false;
    }
}
