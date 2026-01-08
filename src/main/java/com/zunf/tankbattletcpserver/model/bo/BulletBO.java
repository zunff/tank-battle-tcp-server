package com.zunf.tankbattletcpserver.model.bo;

import com.zunf.tankbattletcpserver.grpc.game.match.MatchClientProto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 子弹业务对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulletBO implements Serializable {
    /**
     * 玩家 ID
     */
    private Long playerId;
    /**
     * 子弹 ID
     */
    private Long bulletId;
    /**
     * X坐标 px
     */
    private Double x;
    /**
     * Y坐标 px
     */
    private Double y;
    /**
     * 方向
     */
    private Integer direction;

    /**
     * 速度 px/tick
     */
    private Integer speed;

    /**
     * 伤害
     */
    private Integer damage;

    /**
     * 转换为 Proto 对象
     *
     * @return Proto 对象
     */
    public MatchClientProto.Bullet toProto() {
        return MatchClientProto.Bullet.newBuilder().setPlayerId(playerId).setBulletId(bulletId)
                .setX(x).setY(y).setDirection(direction).build();
    }
}