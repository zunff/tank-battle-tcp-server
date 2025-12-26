package com.zunf.tankbattletcpserver.entity;

import com.zunf.tankbattletcpserver.enums.MatchEndReason;
import com.zunf.tankbattletcpserver.enums.MatchStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class GameMatch {

    // 基本信息
    private Long matchId;          // 对战ID
    private Long roomId;           // 房间ID

    // 地图
    private int[][] mapData;       // 随机生成的地图二维数组

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


    public GameMatch(Long matchId, Long roomId, int[][] mapData, Integer maxPlayers, Integer maxDuration) {
        this.matchId = matchId;
        this.roomId = roomId;
        this.mapData = mapData;
        this.status = MatchStatus.WAITING;
        this.createTime = System.currentTimeMillis();
        this.maxPlayers = maxPlayers;
        this.maxDuration = maxDuration;
        this.initLife = 100;
    }
}
