package com.zunf.tankbattletcpserver.manager;

import com.zunf.tankbattletcpserver.entity.GameMessage;
import io.netty.channel.Channel;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OnlineSessionManager {

    // playerId -> Channel
    private final Map<Long, Channel> playerChannelMap = new ConcurrentHashMap<>();

    public void bind(long playerId, Channel channel) {
        playerChannelMap.put(playerId, channel);
    }

    public void unbind(long playerId, Channel channel) {
        // 防御性：只在 map 中的 channel 与当前一致时才移除
        playerChannelMap.computeIfPresent(playerId, (k, v) -> (v == channel ? null : v));
    }

    public Channel getChannel(long playerId) {
        return playerChannelMap.get(playerId);
    }

    /**
     * 给多个玩家推送同一条消息
     */
    public void pushToPlayers(Collection<Long> playerIds, GameMessage msgTemplate) {
        for (Long playerId : playerIds) {
            Channel ch = playerChannelMap.get(playerId);
            if (ch == null || !ch.isActive()) {
                continue;
            }
            // 每个玩家一份独立的 GameMessage，避免共享对象被修改
            GameMessage msg = cloneForPlayer(msgTemplate, playerId);
            ch.writeAndFlush(msg);
        }
    }

    private GameMessage cloneForPlayer(GameMessage template, long playerId) {
        GameMessage m = new GameMessage(
                template.getMsgType(),
                template.getVersion(),
                template.getRequestId(),
                template.getBody() != null ? template.getBody().clone() : null
        );
        m.setPlayerId(playerId);
        m.setTraceId(template.getTraceId());
        return m;
    }
}
