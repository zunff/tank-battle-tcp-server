package com.zunf.tankbattletcpserver.model.entity.game;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.ObjUtil;
import com.google.protobuf.ByteString;
import com.zunf.tankbattletcpserver.constant.MapConstant;
import com.zunf.tankbattletcpserver.enums.*;
import com.zunf.tankbattletcpserver.model.bo.BulletBO;
import com.zunf.tankbattletcpserver.model.bo.TankBO;
import com.zunf.tankbattletcpserver.model.bo.TickBO;
import com.zunf.tankbattletcpserver.model.bo.WallBO;
import com.zunf.tankbattletcpserver.model.entity.PlayerInMatch;
import com.zunf.tankbattletcpserver.grpc.game.match.MatchClientProto;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Slf4j
@Data
@NoArgsConstructor
public class GameMatch {

    // 基本信息
    private Long matchId;          // 对战ID
    private Long roomId;           // 房间ID
    private AtomicLong bulletIdGenerator = new AtomicLong(1);

    // 地图 & tick
    private GameMapData mapData;   // 随机生成的地图二维数组
    private Integer lastSpawnIndex = 0;
    private Queue<GameMatchOp> operationQueue;
    private ScheduledFuture<?> tickTask;
    private TickBO lastestTick;
    private Consumer<GameMatch> asyncPushTickCallback;

    // 状态 & 时间
    private MatchStatus status;    // WAITING, RUNNING, FINISHED, CANCELED
    private Long createTime;       // 创建时间
    private Long startTime;        // 开始时间
    private Long endTime;          // 结束时间

    // 对局配置
    private Integer maxPlayers;    // 最大玩家数
    private Integer maxDuration;   // 最大时长（秒），到时间就结束
    private Integer initLife;      // 每个玩家初始生命数

    // 参与者
    private List<PlayerInMatch> players;  // 玩家在本局中的信息列表

    // 结果（死斗模式一般是按击杀/得分排序）
    private Long winnerPlayerId;   // 胜利玩家ID（结束时统计）
    private MatchEndReason endReason; // NORMAL, TIMEOUT, ALL_LEFT 等


    public GameMatch(Long matchId, Long roomId, GameMapData mapData, Integer maxPlayers, Integer maxDuration,
                     List<PlayerInMatch> players, Consumer<GameMatch> asyncPushTickCallback) {
        this.matchId = matchId;
        this.roomId = roomId;
        this.mapData = mapData;
        this.status = MatchStatus.WAITING;
        this.createTime = System.currentTimeMillis();
        this.maxPlayers = maxPlayers;
        this.maxDuration = maxDuration;
        this.initLife = 100;
        this.players = players;
        this.operationQueue = new ConcurrentLinkedQueue<>();
        this.asyncPushTickCallback = asyncPushTickCallback;
    }

    public List<ByteString> getMapData() {
        List<ByteString> list = new ArrayList<>();
        for (byte[] mapDatum : mapData.getMapData()) {
            list.add(ByteString.copyFrom(mapDatum));
        }
        return list;
    }

    public ByteString getSpawnPoint(Long playerId) {
        int[] spawnPoint = mapData.getSpawnPoints().get(lastSpawnIndex);
        byte[] spawnPointBytes = new byte[spawnPoint.length];
        for (int i = 0; i < spawnPoint.length; i++) {
            spawnPointBytes[i] = (byte) spawnPoint[i];
        }
        // 标识
        players.stream().filter(playerInMatch -> playerInMatch.getPlayerId().equals(playerId))
                .forEach(playerInMatch -> playerInMatch.setSpawnIndex(lastSpawnIndex++));
        return ByteString.copyFrom(spawnPointBytes);
    }

    public void initTick() {
        this.lastestTick =  TickBO.builder()
                .matchId(matchId)
                .tickTimeStamp(System.currentTimeMillis())
                .tanks(players.stream().map(player -> player.initTank(mapData.getSpawnPoints())).toList())
                .bullets(new ArrayList<>())
                .mapData(mapData.getMapData())
                .isGameOver(false)
                .build();
    }

    public void offerOperation(GameMsgType msgType, Long playerId, MatchClientProto.OpParams params) {
        GameMatchOp operation = new GameMatchOp(msgType, playerId, params);
        operationQueue.offer(operation);
    }

    public void tick() {
        if (status != MatchStatus.RUNNING) {
            return;
        }
        try {
            // 1. 执行核心游戏业务逻辑 并获取最新Tick
            TickBO currentTick = doCoreGameLogic();

            // 2. 缓存最新Tick
            this.lastestTick = currentTick;

            // 3. 异步触发推送
            asyncPushTickCallback.accept(this);
            log.info("Async push tick game match {}", matchId);
        } catch (Exception e) {
            log.error("Failed to tick game match {}", matchId, e);
            this.endReason = MatchEndReason.ERROR;
            stopGameMatch(false);
        }
    }

    /**
     * 核心游戏业务逻辑
     */
    private TickBO doCoreGameLogic() {
        TickBO tick = lastestTick;
        Set<String> playerOpSet = new HashSet<>();
        int size = operationQueue.size();
        // 计算所有存在的子弹位置，判断子弹是否销毁或撞到墙或玩家
        computeBulletTick(tick);
        // 循环处理所有操作
        for (int i = 0; i < size; i++) {
            GameMatchOp operation = operationQueue.poll();
            // 1. 同一个玩家在一个tick内只能执行一次相同操作
            String uuid = operation.getPlayerId() + "_" + operation.getMsgType().getCode();
            if (playerOpSet.contains(uuid)) {
                continue;
            }
            TankBO tank = getTankByPlayerId(tick, operation.getPlayerId());
            if (tank == null) {
                continue;
            }

            // 2. 根据操作类型执行逻辑
            computeTickByMsgType(operation, tank, tick);
            playerOpSet.add(uuid);
        }

        //  检测对局结束条件，满足则停止
        boolean matchOver = isMatchOver();
        if (matchOver) {
            stopGameMatch(true);
        }
        tick.setIsGameOver(matchOver);
        return tick;
    }

    private void computeBulletTick(TickBO tick) {
        // 先计算这个 tick 的子弹位置，把要删除的子弹 ID 缓存起来
        Set<Long> deleteBulletIds = new HashSet<>();
        for (BulletBO bullet : tick.getBullets()) {
            // 先计算 子弹位置
            Pair<Double, Double> pair = computeIndexByDirectionAndSpeed(bullet.getX(), bullet.getY(), bullet.getDirection(), bullet.getSpeed());
            Double x = pair.getKey();
            Double y = pair.getValue();
            if (isOutOfMap(x, y)) {
                deleteBulletIds.add(bullet.getBulletId());
                continue;
            }
            WallBO wall = isHitWall(x, y);
            if (wall != null) {
                deleteBulletIds.add(bullet.getBulletId());
                // 如果墙可以摧毁，这里处理成已摧毁
                if (wall.getType() == MapIndex.WALL) {
                    byte[][] map = mapData.getMapData();
                    map[wall.getY()][wall.getX()] = MapIndex.DESTROYED_WALL.getCode();
                }
                continue;
            }
            Long hitTankPlayerId = isHitTank(x, y, tick.getTanks(), bullet.getPlayerId());
            if (hitTankPlayerId != null) {
                deleteBulletIds.add(bullet.getBulletId());
                // 击中坦克 扣血
                TankBO hitTank = getTankByPlayerId(tick, hitTankPlayerId);
                hitTank.setLife(hitTank.getLife() - bullet.getDamage());
                continue;
            }
            bullet.setX(x);
            bullet.setY(y);
        }

        // 删除要删除的子弹
        tick.getBullets().removeIf(bullet -> deleteBulletIds.contains(bullet.getBulletId()));
    }


    private boolean isOutOfMap(double x, double y) {
        return x < 0 || x >= MapConstant.MAP_PX || y < 0 || y >= MapConstant.MAP_PY;
    }

    /**
     * 检测 x,y 是否在坦克内，如果撞到了，返回坦克的 ID
     * 单位 px
     */

    private Long isHitTank(double x, double y, List<TankBO> tanks, long playerId) {
        for (TankBO tank : tanks) {
            // 忽略自己
            if (tank.getPlayerId() == playerId) {
                continue;
            }
            double tankX = tank.getX();
            double tankY = tank.getY();
            double tankWidthHalf;
            double tankHeightHalf;
            
            // 根据坦克方向确定碰撞检测的宽高
            if (tank.getDirection() == Direction.UP.getCode() || tank.getDirection() == Direction.DOWN.getCode()) {
                // 上下方向：高度为长边，宽度为短边
                tankHeightHalf = MapConstant.TANK_LONG_SIDE / 2;
                tankWidthHalf = MapConstant.TANK_SHORT_SIDE / 2;
            } else {
                // 左右方向：宽度为长边，高度为短边
                tankWidthHalf = MapConstant.TANK_LONG_SIDE / 2;
                tankHeightHalf = MapConstant.TANK_SHORT_SIDE / 2;
            }

            if (x >= tankX - tankWidthHalf && x < tankX + tankWidthHalf && y >= tankY - tankHeightHalf && y < tankY + tankHeightHalf) {
                return tank.getPlayerId();
            }
        }
        return null;
    }

    /**
     * 计算x,y 是否在墙内，如果撞到了，返回方块的类型
     * 单位 px
     */
    private WallBO isHitWall(double x, double y) {
        byte[][] map = mapData.getMapData();
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                byte code = map[i][j];
                if (!MapIndex.of(code).isWall()) {
                    continue;
                }
                // 拿到方块的左上角 px坐标
                int leftTopX = j * MapConstant.GRID_SIZE;
                int leftTopY = i * MapConstant.GRID_SIZE;
                if (x >= leftTopX && x < leftTopX + MapConstant.GRID_SIZE && y >= leftTopY && y < leftTopY + MapConstant.GRID_SIZE) {
                    // j 是列，i 是行
                    return new WallBO(MapIndex.of(code), j, i);
                }
            }
        }
        return null;
    }

    private void computeTickByMsgType(GameMatchOp operation, TankBO tank, TickBO tick) {
        switch (operation.getMsgType()) {
            case TANK_MOVE:
                tank.setDirection(operation.getParams().getTankDirection());
                Pair<Double, Double> centerIndex = computeIndexByDirectionAndSpeed(tank.getX(), tank.getY(), tank.getDirection(), tank.getSpeed());
                // 根据坦克方向拿到一个碰撞检测坐标
                List<Pair<Double, Double>> hitCheckIndexList = computeHitCheckIndex(centerIndex, tank.getDirection());
                // 判断是否撞墙或撞到敌方坦克，撞到了不让他移动
                if (isTankHitSomething(hitCheckIndexList, tank, tick)) {
                    return;
                }
                tank.setX(centerIndex.getKey());
                tank.setY(centerIndex.getValue());
                break;
            case TANK_SHOOT:
                long bulletId = bulletIdGenerator.getAndIncrement();
                BulletBO bullet = BulletBO.builder()
                        .bulletId(bulletId)
                        .playerId(operation.getPlayerId())
                        .x(tank.getX())
                        .y(tank.getY())
                        .direction(tank.getDirection())
                        .speed(18)
                        .damage(10)
                        .build();
                tick.getBullets().add(bullet);
                break;
            default:
                break;
        }
    }

    private boolean isTankHitSomething(List<Pair<Double, Double>> hitCheckIndexList, TankBO tank, TickBO tick) {
        for (Pair<Double, Double> hitCheckIndex : hitCheckIndexList) {
            if (hitCheckIndex == null || isOutOfMap(hitCheckIndex.getKey(), hitCheckIndex.getValue())
                    || isHitTank(hitCheckIndex.getKey(), hitCheckIndex.getValue(), tick.getTanks(), tank.getPlayerId()) != null
                    || isHitWall(hitCheckIndex.getKey(), hitCheckIndex.getValue()) != null) {
                return true;
            }
        }
        return false;
    }

    private List<Pair<Double, Double>> computeHitCheckIndex(Pair<Double, Double> pair, Integer direction) {
        double tankLongSideHalf = MapConstant.TANK_LONG_SIDE / 2;
        double tankShortSideHalf = MapConstant.TANK_SHORT_SIDE / 2;

        return switch (Direction.of(direction)) {
            case UP ->
                    List.of(Pair.of(pair.getKey() - tankShortSideHalf, pair.getValue() - tankLongSideHalf), Pair.of(pair.getKey() + tankShortSideHalf, pair.getValue() - tankLongSideHalf));
            case DOWN ->
                    List.of(Pair.of(pair.getKey() + tankShortSideHalf, pair.getValue() + tankLongSideHalf), Pair.of(pair.getKey() - tankShortSideHalf, pair.getValue() + tankLongSideHalf));
            case LEFT ->
                    List.of(Pair.of(pair.getKey() - tankLongSideHalf, pair.getValue() - tankShortSideHalf), Pair.of(pair.getKey() - tankLongSideHalf, pair.getValue() + tankShortSideHalf));
            case RIGHT ->
                    List.of(Pair.of(pair.getKey() + tankLongSideHalf, pair.getValue() + tankShortSideHalf), Pair.of(pair.getKey() + tankLongSideHalf, pair.getValue() - tankShortSideHalf));
        };
    }

    private Pair<Double, Double> computeIndexByDirectionAndSpeed(double x, double y, int direction, int speed) {
        switch (Direction.of(direction)) {
            case UP:
                y -= speed;
                break;
            case DOWN:
                y += speed;
                break;
            case LEFT:
                x -= speed;
                break;
            case RIGHT:
                x += speed;
                break;
        }
        return new Pair<>(x, y);
    }


    private TankBO getTankByPlayerId(TickBO tick, Long playerId) {
        return tick.getTanks().stream().filter(tank -> ObjUtil.equals(tank.getPlayerId(), playerId)).findFirst().orElse(null);
    }

    private boolean isMatchOver() {
        // 1. 超时
        if (System.currentTimeMillis() - startTime > maxDuration * 1000) {
            this.endReason = MatchEndReason.TIMEOUT;
            return true;
        }

        // 2. 全部玩家已离开
        if (players.stream().noneMatch(PlayerInMatch::getOnline)) {
            this.endReason = MatchEndReason.ALL_LEFT;
            return true;
        }
        return false;
    }

    /**
     * 停止tick任务+对局销毁
     */
    private void stopGameMatch(boolean normalEnd) {
        // 1. 标记对局为，避免后续tick重复执行
        this.status = normalEnd ? MatchStatus.FINISHED : MatchStatus.CANCELED;

        // 2. 自我取消tick定时任务（核心：操作内部持有的tickFuture）
        if (this.tickTask != null && !this.tickTask.isCancelled()) {
            // false：不中断正在执行的当前tick任务，保证业务完整性
            this.tickTask.cancel(false);
        }

        // todo 3. 执行对局结束结算逻辑
    }
}
