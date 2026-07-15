package com.conceptarena.app.voice;

import com.conceptarena.app.bus.CommandHandler;
import com.conceptarena.app.bus.EventBus;
import com.conceptarena.core.voice.command.SignalCommand;
import com.conceptarena.core.voice.event.VoiceConnected;
import com.conceptarena.core.voice.event.VoiceDisconnected;
import org.springframework.stereotype.Service;

@Service
public class HandleSignalCommandHandler implements CommandHandler<SignalCommand, Void> {

    private final EventBus eventBus;

    public HandleSignalCommandHandler(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public Void handle(SignalCommand command) {
        if ("connect".equals(command.type())) {
            eventBus.publish(new VoiceConnected(command.roomId(), command.roomId(), command.fromUserId()));
        } else if ("disconnect".equals(command.type())) {
            eventBus.publish(new VoiceDisconnected(command.roomId(), command.roomId(), command.fromUserId()));
        }
        return null;
    }
}
