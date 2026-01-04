package com.zunf.tankbattletcpserver.model.entity.game;

import com.zunf.tankbattletcpserver.enums.GameMsgType;
import com.zunf.tankbattletcpserver.grpc.game.match.MatchClientProto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对局内的操作
 *
 * @author zunf
 * @date 2026/1/2 18:21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameMatchOp {

    private GameMsgType msgType;

    private long playerId;

    private MatchClientProto.OpParams params;

}
