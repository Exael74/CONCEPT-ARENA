package com.conceptarena.web.ws;

import com.conceptarena.app.bus.CommandBus;
import com.conceptarena.core.voice.command.SignalCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class SignalingWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SignalingWebSocketHandler.class);
    private final CommandBus commandBus;
    private final ObjectMapper objectMapper;

    public SignalingWebSocketHandler(CommandBus commandBus, ObjectMapper objectMapper) {
        this.commandBus = commandBus;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        SignalingMessage msg = objectMapper.readValue(message.getPayload(), SignalingMessage.class);
        commandBus.dispatch(new SignalCommand(
            msg.roomId(), msg.fromUserId(), msg.toUserId(), msg.type(), msg.payload()
        ));
    }

    private record SignalingMessage(String type, String roomId, String fromUserId, String toUserId, String payload) {}
}
