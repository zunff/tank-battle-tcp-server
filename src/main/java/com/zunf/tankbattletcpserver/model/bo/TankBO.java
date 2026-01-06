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
    private Integer speed;

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

    /**
     * 从 Proto 对象创建BO对象
     *
     * @param proto Proto 对象
     * @return BO 对象
     */
    public static TankBO fromProto(MatchClientProto.Tank proto) {
        TankBO bo = new TankBO();
        bo.setPlayerId(proto.getPlayerId());
        bo.setX(proto.getX());
        bo.setY(proto.getY());
        bo.setDirection(proto.getDirection());
        return bo;
    }
}