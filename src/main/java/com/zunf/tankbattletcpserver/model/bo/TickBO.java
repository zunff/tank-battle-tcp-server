package com.zunf.tankbattletcpserver.model.bo;

import com.google.protobuf.ByteString;
import com.zunf.tankbattletcpserver.enums.MatchEndReason;
import com.zunf.tankbattletcpserver.grpc.game.match.MatchClientProto;
import com.zunf.tankbattletcpserver.model.entity.PlayerInMatch;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tick 数据业务对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TickBO implements Serializable {
    /**
     * 匹配 ID
     */
    private Long matchId;
    /**
     * Tick 时间戳
     */
    private Long tickTimeStamp;
    /**
     * 坦克列表
     */
    private List<TankBO> tanks;
    /**
     * 子弹列表
     */
    private List<BulletBO> bullets;
    /**
     * 地图数据
     */
    private byte[][] mapData;
    /**
     * 游戏是否结束
     */
    private Boolean isGameOver;

    /**
     * 获胜玩家 ID
     */
    private Long winnerPlayerId;

    /**
     * 游戏结束原因
     */
    private MatchEndReason endReason;



    /**
     * 转换为 Proto 对象
     *
     * @return Proto 对象
     */
    public MatchClientProto.Tick toProto() {
        MatchClientProto.Tick.Builder builder = MatchClientProto.Tick.newBuilder()
                .setMatchId(matchId).setTickTimeStamp(tickTimeStamp).setIsGameOver(isGameOver)
                .setWinnerPlayerId(winnerPlayerId).setEndReason(endReason.getCode());

        if (tanks != null) {
            for (TankBO tank : tanks) {
                builder.addTanks(tank.toProto());
            }
        }
        if (bullets != null) {
            for (BulletBO bullet : bullets) {
                builder.addBullets(bullet.toProto());
            }
        }
        if (mapData != null) {
            for (byte[] mapDatum : mapData) {
                builder.addMapData(ByteString.copyFrom(mapDatum));
            }
        }
        return builder.build();
    }


    public List<TankBO> getOnlineTanks(List<PlayerInMatch> players) {
        Map<Long, Boolean> playerOnlineMap = players.stream().collect(Collectors.toMap(PlayerInMatch::getPlayerId, PlayerInMatch::getOnline));
        return tanks.stream().filter(
                tank -> playerOnlineMap.getOrDefault(tank.getPlayerId(), false)
        ).collect(Collectors.toList());
    }
}