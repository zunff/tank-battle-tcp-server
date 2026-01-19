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

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
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

        private static final Random random = new Random();

        private List<int[]> generateSpawnPoints(int width, int height, int maxPlayers) {
            List<int[]> points = new ArrayList<>();
            int margin = MapConstant.SPAWN_MARGIN;

            // 确定最终要生成的点数：最小2个，无上限（和玩家数一致）
            int targetCount = Math.max(maxPlayers, 2);

            // 生成指定数量的重生点，确保点之间距离足够远
            for (int i = 0; i < targetCount; i++) {
                int[] newPoint = null;
                int retryCount = 0;

                // 循环生成点，直到找到符合距离要求的点，或重试次数耗尽
                while (newPoint == null && retryCount < MapConstant.MAX_RETRY_PER_POINT) {
                    // 在地图有效区域内随机生成点（避开边缘）
                    int x = margin + random.nextInt(width - 2 * margin);
                    int y = margin + random.nextInt(height - 2 * margin);
                    int[] candidate = new int[]{x, y};

                    // 检查新点与所有已有点的距离是否符合要求
                    boolean isFarEnough = true;
                    for (int[] existingPoint : points) {
                        double distance = calculateDistance(candidate, existingPoint);
                        if (distance < MapConstant.MIN_SPAWN_DISTANCE) {
                            isFarEnough = false;
                            break;
                        }
                    }

                    // 如果距离符合要求，确定这个新点
                    if (isFarEnough) {
                        newPoint = candidate;
                    }
                    retryCount++;
                }

                // 如果重试耗尽仍未找到有效点，使用当前最接近的点（兜底策略）
                if (newPoint == null) {
                    newPoint = new int[]{margin + random.nextInt(width - 2 * margin),
                            margin + random.nextInt(height - 2 * margin)};
                }

                points.add(newPoint);
            }

            return points;
        }

        /**
         * 计算两个点之间的欧几里得距离
         *
         * @param point1 第一个点 [x1, y1]
         * @param point2 第二个点 [x2, y2]
         * @return 两点之间的距离
         */
        private double calculateDistance(int[] point1, int[] point2) {
            int dx = point1[0] - point2[0];
            int dy = point1[1] - point2[1];
            return Math.sqrt(dx * dx + dy * dy);
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
