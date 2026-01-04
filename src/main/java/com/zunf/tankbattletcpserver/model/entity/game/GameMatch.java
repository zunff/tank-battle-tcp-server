package com.zunf.tankbattletcpserver.model.entity.game;

import com.google.protobuf.ByteString;
import com.zunf.tankbattletcpserver.manager.GameMatchManager;
import com.zunf.tankbattletcpserver.model.entity.PlayerInMatch;
import com.zunf.tankbattletcpserver.enums.GameMsgType;
import com.zunf.tankbattletcpserver.enums.MatchEndReason;
import com.zunf.tankbattletcpserver.enums.MatchStatus;
import com.zunf.tankbattletcpserver.grpc.game.match.MatchClientProto;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

@Slf4j
@Data
@NoArgsConstructor
public class GameMatch {

    // 基本信息
    private Long matchId;          // 对战ID
    private Long roomId;           // 房间ID

    // 地图 & tick
    private GameMapData mapData;   // 随机生成的地图二维数组
    private Integer lastSpawnIndex = 0;
    private Queue<GameMatchOp> operationQueue;
    private ScheduledFuture<?> tickTask;
    private MatchClientProto.Tick lastestTick;
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
        this.lastestTick =  MatchClientProto.Tick.newBuilder()
                .setMatchId(matchId)
                .setTickTimeStamp(System.currentTimeMillis())
                .addAllTanks(players.stream().map(PlayerInMatch::toTank).toList())
                .addAllBullets(new ArrayList<>())
                .addAllMapData(getMapData())
                .setIsGameOver(false)
                .build();
        // 推送初始 Tick
        asyncPushTickCallback.accept(this);
    }

    public void offerOperation(GameMsgType msgType, Long playerId, MatchClientProto.OpParams params) {
        GameMatchOp operation = new GameMatchOp(msgType, playerId, params);
        operationQueue.offer(operation);
    }

    public void tick() {
        if (operationQueue.isEmpty() || status != MatchStatus.RUNNING) {
            return;
        }
        try {
            // 1. 执行核心游戏业务逻辑 并获取最新Tick
            MatchClientProto.Tick currentTick = doCoreGameLogic();

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
    private MatchClientProto.Tick doCoreGameLogic() {
        MatchClientProto.Tick.Builder tickBuilder = MatchClientProto.Tick.newBuilder();
        Set<String> playerOpSet = new HashSet<>();
        int size = operationQueue.size();
        for (int i = 0; i < size; i++) {
            GameMatchOp operation = operationQueue.poll();
            // 1. 同一个玩家在一个tick内只能执行一次相同操作
            String uuid = operation.getPlayerId() + "_" + operation.getMsgType().getCode();
            if (playerOpSet.contains(uuid)) {
                continue;
            }
            // 2. 根据操作类型执行逻辑
            switch (operation.getMsgType()) {
                case TANK_MOVE:
                    break;
                case TANK_SHOOT:
                    break;
                default:
                    break;
            }
            playerOpSet.add(uuid);
        }

        // 2. 检测对局结束条件，满足则停止
        boolean matchOver = isMatchOver();
        if (matchOver) {
            stopGameMatch(true);
        }
        return tickBuilder.setIsGameOver(matchOver).build();
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
