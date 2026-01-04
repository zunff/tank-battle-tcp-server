package com.zunf.tankbattletcpserver.manager;

import com.zunf.tankbattletcpserver.model.entity.game.GameMessage;
import io.netty.channel.Channel;
import org.springframework.stereotype.Component;

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
    public void pushToPlayer(Long playerId, GameMessage msg) {
        Channel ch = playerChannelMap.get(playerId);
        if (ch == null || !ch.isActive()) {
            return;
        }
        ch.writeAndFlush(msg);
    }
}
