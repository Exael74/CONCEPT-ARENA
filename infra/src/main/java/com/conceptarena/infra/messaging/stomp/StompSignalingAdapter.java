package com.conceptarena.infra.messaging.stomp;

import com.conceptarena.app.bus.EventBus;
import com.conceptarena.app.bus.EventHandler;
import com.conceptarena.core.voice.event.VoiceConnected;
import com.conceptarena.core.voice.event.VoiceDisconnected;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompSignalingAdapter {

    private final EventBus eventBus;
    private final SimpMessagingTemplate messaging;

    public StompSignalingAdapter(EventBus eventBus, SimpMessagingTemplate messaging) {
        this.eventBus = eventBus;
        this.messaging = messaging;
    }

    @PostConstruct
    public void subscribe() {
        eventBus.subscribe(VoiceConnected.class, (EventHandler<VoiceConnected>) this::onVoiceConnected);
        eventBus.subscribe(VoiceDisconnected.class, (EventHandler<VoiceDisconnected>) this::onVoiceDisconnected);
    }

    private void onVoiceConnected(VoiceConnected event) {
        messaging.convertAndSend("/topic/rooms/" + event.getRoomId() + "/signaling", Map.of(
            "type", "VOICE_CONNECTED",
            "userId", event.getUserId()
        ));
    }

    private void onVoiceDisconnected(VoiceDisconnected event) {
        messaging.convertAndSend("/topic/rooms/" + event.getRoomId() + "/signaling", Map.of(
            "type", "VOICE_DISCONNECTED",
            "userId", event.getUserId()
        ));
    }
}

