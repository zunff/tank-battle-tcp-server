package com.zunf.tankbattletcpserver.model.bo;

import com.google.protobuf.ByteString;
import com.zunf.tankbattletcpserver.grpc.game.match.MatchClientProto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
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
     * 转换为 Proto 对象
     *
     * @return Proto 对象
     */
    public MatchClientProto.Tick toProto() {
        MatchClientProto.Tick.Builder builder = MatchClientProto.Tick.newBuilder();
        if (matchId != null) {
            builder.setMatchId(matchId);
        }
        if (tickTimeStamp != null) {
            builder.setTickTimeStamp(tickTimeStamp);
        }
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
        if (isGameOver != null) {
            builder.setIsGameOver(isGameOver);
        }
        return builder.build();
    }

    /**
     * 从 Proto 对象创建BO对象
     *
     * @param proto Proto 对象
     * @return BO 对象
     */
    public static TickBO fromProto(MatchClientProto.Tick proto) {
        TickBO bo = new TickBO();
        bo.setMatchId(proto.getMatchId());
        bo.setTickTimeStamp(proto.getTickTimeStamp());
        
        // 转换坦克列表
        if (proto.getTanksCount() > 0) {
            List<TankBO> tanks = proto.getTanksList().stream()
                    .map(TankBO::fromProto)
                    .collect(Collectors.toList());
            bo.setTanks(tanks);
        }
        
        // 转换子弹列表
        if (proto.getBulletsCount() > 0) {
            List<BulletBO> bullets = proto.getBulletsList().stream()
                    .map(BulletBO::fromProto)
                    .collect(Collectors.toList());
            bo.setBullets(bullets);
        }
        
        // 转换地图数据
        if (proto.getMapDataCount() > 0) {
            byte[][] mapData = proto.getMapDataList().stream()
                    .map(ByteString::toByteArray)
                    .toList().toArray(byte[][]::new);
            bo.setMapData(mapData);
        }
        
        bo.setIsGameOver(proto.getIsGameOver());
        return bo;
    }
}