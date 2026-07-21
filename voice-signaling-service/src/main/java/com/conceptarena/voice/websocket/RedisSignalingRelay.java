package com.conceptarena.voice.websocket;

import com.conceptarena.voice.dto.SignalingMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * S2: cross-replica signaling relay over Redis Pub/Sub.
 *
 * A WebRTC signaling message can only be delivered by the replica the TARGET peer's socket is
 * connected to. SignalingWebSocketHandler delivers directly when the target is local; when it is
 * NOT local (a different replica holds that session), it hands the message here. This publishes it
 * to a single Redis channel; every replica subscribes, and whichever one actually holds the target
 * session delivers it. So presence stays local (a live socket can't be shared), but the *message*
 * fans out to reach it — closing the "user on replica A can't signal user on replica B" gap.
 *
 * Active only under the docker profile (app.signaling.relay.enabled=true); the default/test profile
 * is single-instance and this bean is absent, so no Redis is required there.
 */
@Component
@ConditionalOnProperty(name = "app.signaling.relay.enabled", havingValue = "true")
public class RedisSignalingRelay implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisSignalingRelay.class);
    private static final String CHANNEL = "signaling:relay";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final SignalingPresenceRegistry presenceRegistry;

    public RedisSignalingRelay(StringRedisTemplate redis, ObjectMapper objectMapper,
                               SignalingPresenceRegistry presenceRegistry,
                               RedisMessageListenerContainer listenerContainer) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.presenceRegistry = presenceRegistry;
        listenerContainer.addMessageListener(this, new ChannelTopic(CHANNEL));
    }

    /** Publish a message whose target isn't connected to this replica, for whichever replica holds it. */
    public void publish(SignalingMessage message) {
        try {
            redis.convertAndSend(CHANNEL, objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            log.warn("Failed to publish signaling relay message for room {}: {}", message.roomId(), e.getMessage());
        }
    }

    /** Delivered on every replica; only the one holding the target session actually sends it. */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            SignalingMessage msg = objectMapper.readValue(message.getBody(), SignalingMessage.class);
            WebSocketSession target = presenceRegistry.find(msg.roomId(), msg.toUserId());
            if (target != null && target.isOpen()) {
                // Deliver with the same shape local delivery uses (toUserId nulled — it's on their socket).
                SignalingMessage outbound = new SignalingMessage(msg.type(), msg.roomId(), null, msg.fromUserId(), msg.payload());
                target.sendMessage(new TextMessage(objectMapper.writeValueAsString(outbound)));
            }
        } catch (IOException e) {
            log.warn("Failed to deliver relayed signaling message: {}", e.getMessage());
        }
    }

    /** The message listener container the relay registers itself on (docker profile only). */
    @Component
    @ConditionalOnProperty(name = "app.signaling.relay.enabled", havingValue = "true")
    static class ListenerContainerConfig {
        @org.springframework.context.annotation.Bean
        RedisMessageListenerContainer signalingListenerContainer(RedisConnectionFactory connectionFactory) {
            RedisMessageListenerContainer container = new RedisMessageListenerContainer();
            container.setConnectionFactory(connectionFactory);
            return container;
        }
    }
}
