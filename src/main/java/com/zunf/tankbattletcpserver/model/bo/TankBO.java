package com.zunf.tankbattletcpserver.model.bo;

import com.zunf.tankbattletcpserver.grpc.game.match.MatchClientProto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 坦克业务对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TankBO implements Serializable {
    /**
     * 玩家 ID
     */
    private Long playerId;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * X坐标
     */
    private Double x;
    /**
     * Y坐标
     */
    private Double y;
    /**
     * 方向
     */
    private Integer direction;

    /**
     * 速度 px/tick
     */
    private Double speed;

    /**
     * 生命值
     */
    private Integer life;

    /**
     * 转换为 Proto 对象
     *
     * @return Proto 对象
     */
    public MatchClientProto.Tank toProto() {
        MatchClientProto.Tank.Builder builder = MatchClientProto.Tank.newBuilder();
        if (playerId != null) {
            builder.setPlayerId(playerId);
        }
        if (nickname != null) {
            builder.setNickName(nickname);
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
        if (life != null) {
            builder.setLife(life);
        }
        return builder.build();
    }
}