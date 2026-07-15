package com.conceptarena.room.infra.config;

import com.conceptarena.room.app.CreateRoomCommandHandler;
import com.conceptarena.room.app.JoinRoomCommandHandler;
import com.conceptarena.room.app.LeaveRoomCommandHandler;
import com.conceptarena.room.domain.command.CreateRoomCommand;
import com.conceptarena.room.domain.command.JoinRoomCommand;
import com.conceptarena.room.domain.command.LeaveRoomCommand;
import com.conceptarena.room.infra.bus.InMemoryCommandBus;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusConfig {

    private final InMemoryCommandBus commandBus;
    private final CreateRoomCommandHandler createRoomHandler;
    private final JoinRoomCommandHandler joinRoomHandler;
    private final LeaveRoomCommandHandler leaveRoomHandler;

    public BusConfig(InMemoryCommandBus commandBus, CreateRoomCommandHandler createRoomHandler,
                      JoinRoomCommandHandler joinRoomHandler, LeaveRoomCommandHandler leaveRoomHandler) {
        this.commandBus = commandBus;
        this.createRoomHandler = createRoomHandler;
        this.joinRoomHandler = joinRoomHandler;
        this.leaveRoomHandler = leaveRoomHandler;
    }

    @PostConstruct
    public void registerHandlers() {
        commandBus.register(CreateRoomCommand.class, createRoomHandler);
        commandBus.register(JoinRoomCommand.class, joinRoomHandler);
        commandBus.register(LeaveRoomCommand.class, leaveRoomHandler);
    }
}
