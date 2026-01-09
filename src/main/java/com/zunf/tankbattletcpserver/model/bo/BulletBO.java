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
        MatchClientProto.Bullet.Builder builder = MatchClientProto.Bullet.newBuilder();
        if (playerId != null) {
            builder.setPlayerId(playerId);
        }
        if (bulletId != null) {
            builder.setBulletId(bulletId);
        }
        if (x != null) {
            builder.setX(x);
        }
        if (y != null) {
            builder.setY(y);
        }
        if (direction != null) {
            builder.setDirection(direction);
        }
        return builder.build();
    }
}