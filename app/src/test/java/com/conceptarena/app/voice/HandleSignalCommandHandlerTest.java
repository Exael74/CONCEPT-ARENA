package com.conceptarena.app.voice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.conceptarena.app.bus.EventBus;
import com.conceptarena.core.voice.command.SignalCommand;
import com.conceptarena.core.voice.event.VoiceConnected;
import com.conceptarena.core.voice.event.VoiceDisconnected;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HandleSignalCommandHandlerTest {

    @Mock private EventBus eventBus;

    private HandleSignalCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new HandleSignalCommandHandler(eventBus);
    }

    @Test
    void publishesVoiceConnectedOnConnectSignal() {
        handler.handle(new SignalCommand("room-1", "user-1", null, "connect", null));
        verify(eventBus).publish(any(VoiceConnected.class));
    }

    @Test
    void publishesVoiceDisconnectedOnDisconnectSignal() {
        handler.handle(new SignalCommand("room-1", "user-1", null, "disconnect", null));
        verify(eventBus).publish(any(VoiceDisconnected.class));
    }

    @Test
    void doesNotPublishForUnrecognizedSignalType() {
        handler.handle(new SignalCommand("room-1", "user-1", "user-2", "offer", "sdp-payload"));
        verify(eventBus, never()).publish(any());
    }
}
