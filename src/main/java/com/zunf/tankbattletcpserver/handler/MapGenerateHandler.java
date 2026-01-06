package com.zunf.tankbattletcpserver.handler;

import com.zunf.tankbattletcpserver.constant.MapConstant;
import com.zunf.tankbattletcpserver.model.entity.game.GameMapData;
import com.zunf.tankbattletcpserver.enums.MapIndex;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Component
public class MapGenerateHandler {

    public GameMapData generateMap(int maxPlayers) {
        GameMapData gameMapData = new GameMapData();

        int width = MapConstant.WIDTH;
        int height = MapConstant.HEIGHT;
        byte[][] map = new byte[height][width];

        // 初始化为空地
        for (int y = 0; y < height; y++) {
            Arrays.fill(map[y], MapIndex.EMPTY.getCode());
        }

        // 边界墙
        for (int x = 0; x < width; x++) {
            map[0][x] = MapIndex.WALL.getCode();
            map[height - 1][x] = MapIndex.WALL.getCode();
        }
        for (int y = 0; y < height; y++) {
            map[y][0] = MapIndex.WALL.getCode();
            map[y][width - 1] = MapIndex.WALL.getCode();
        }

        // 出生点
        List<int[]> spawnPoints = generateSpawnPoints(width, height, maxPlayers);
        gameMapData.setSpawnPoints(spawnPoints);
        for (int[] p : spawnPoints) {
            int sx = p[0], sy = p[1];
            map[sy][sx] = MapIndex.SPAWN.getCode();
            clearAround(map, sx, sy, 1);
        }

        // 随机障碍
        Random random = new Random();
        double brickRate = MapConstant.BRICK_RATE;
        double wallRate = MapConstant.WALL_RATE;

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (isNearSpawn(x, y, spawnPoints, 2)) {
                    continue;
                }
                double r = random.nextDouble();
                if (r < wallRate) {
                    map[y][x] = MapIndex.WALL.getCode();
                } else if (r < wallRate + brickRate) {
                    map[y][x] = MapIndex.BRICK.getCode();
                }
            }
        }
        gameMapData.setMapData(map);
        return gameMapData;
    }

    private List<int[]> generateSpawnPoints(int width, int height, int maxPlayers) {
        List<int[]> points = new ArrayList<>();
        int m = MapConstant.SPAWN_MARGIN;

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
                map[y][x] = MapIndex.EMPTY.getCode();
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
