package com.zunf.tankbattletcpserver.entity;

import lombok.Data;

@Data
public class PlayerInMatch {
    /**
     * 基本信息
     */
    private Long playerId;
    
    /**
     * 冗余一份昵称，方便直接展示
     */
    private String nickname;
    
    /**
     * 坦克类型/皮肤ID
     */
    private Integer tankType;
    
    /**
     * 出生点索引
     */
    private Integer spawnIndex;

    /**
     * 对局内状态
     */
    private Integer life;          // 当前剩余生命数
    private Integer killCount;     // 击杀数
    private Integer deathCount;    // 死亡数

    private Boolean online;        // 是否仍在线

    /**
     * 其他可选
     */
    private Long lastRespawnTime;  // 上次复活时间

    public PlayerInMatch(GameRoomPlayer player) {
        this.playerId = player.getId();
        this.life = 100;
        this.killCount = 0;
        this.deathCount = 0;
        this.online = true;
    }
}
