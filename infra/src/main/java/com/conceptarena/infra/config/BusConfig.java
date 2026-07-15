package com.conceptarena.infra.config;

import com.conceptarena.app.bus.CommandBus;
import com.conceptarena.app.bus.CommandHandler;
import com.conceptarena.app.concept.CreateConceptBankCommandHandler;
import com.conceptarena.app.game.StartRoundCommandHandler;
import com.conceptarena.app.game.SubmitAnswerCommandHandler;
import com.conceptarena.app.room.CreateRoomCommandHandler;
import com.conceptarena.app.room.JoinRoomCommandHandler;
import com.conceptarena.app.room.LeaveRoomCommandHandler;
import com.conceptarena.app.user.LoginUserCommandHandler;
import com.conceptarena.app.user.RegisterUserCommandHandler;
import com.conceptarena.app.voice.HandleSignalCommandHandler;
import com.conceptarena.core.concept.command.CreateConceptBankCommand;
import com.conceptarena.core.game.command.StartRoundCommand;
import com.conceptarena.core.game.command.SubmitAnswerCommand;
import com.conceptarena.core.room.command.CreateRoomCommand;
import com.conceptarena.core.room.command.JoinRoomCommand;
import com.conceptarena.core.room.command.LeaveRoomCommand;
import com.conceptarena.core.user.command.LoginUserCommand;
import com.conceptarena.core.user.command.RegisterUserCommand;
import com.conceptarena.core.voice.command.SignalCommand;
import com.conceptarena.infra.bus.InMemoryCommandBus;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusConfig {

    private final InMemoryCommandBus commandBus;
    private final RegisterUserCommandHandler registerUserHandler;
    private final LoginUserCommandHandler loginUserHandler;
    private final CreateRoomCommandHandler createRoomHandler;
    private final JoinRoomCommandHandler joinRoomHandler;
    private final LeaveRoomCommandHandler leaveRoomHandler;
    private final StartRoundCommandHandler startRoundHandler;
    private final SubmitAnswerCommandHandler submitAnswerHandler;
    private final CreateConceptBankCommandHandler createConceptBankHandler;
    private final HandleSignalCommandHandler handleSignalHandler;

    public BusConfig(InMemoryCommandBus commandBus,
                     RegisterUserCommandHandler registerUserHandler,
                     LoginUserCommandHandler loginUserHandler,
                     CreateRoomCommandHandler createRoomHandler,
                     JoinRoomCommandHandler joinRoomHandler,
                     LeaveRoomCommandHandler leaveRoomHandler,
                     StartRoundCommandHandler startRoundHandler,
                     SubmitAnswerCommandHandler submitAnswerHandler,
                     CreateConceptBankCommandHandler createConceptBankHandler,
                     HandleSignalCommandHandler handleSignalHandler) {
        this.commandBus = commandBus;
        this.registerUserHandler = registerUserHandler;
        this.loginUserHandler = loginUserHandler;
        this.createRoomHandler = createRoomHandler;
        this.joinRoomHandler = joinRoomHandler;
        this.leaveRoomHandler = leaveRoomHandler;
        this.startRoundHandler = startRoundHandler;
        this.submitAnswerHandler = submitAnswerHandler;
        this.createConceptBankHandler = createConceptBankHandler;
        this.handleSignalHandler = handleSignalHandler;
    }

    @PostConstruct
    public void registerHandlers() {
        commandBus.register(RegisterUserCommand.class, registerUserHandler);
        commandBus.register(LoginUserCommand.class, loginUserHandler);
        commandBus.register(CreateRoomCommand.class, createRoomHandler);
        commandBus.register(JoinRoomCommand.class, joinRoomHandler);
        commandBus.register(LeaveRoomCommand.class, leaveRoomHandler);
        commandBus.register(StartRoundCommand.class, startRoundHandler);
        commandBus.register(SubmitAnswerCommand.class, submitAnswerHandler);
        commandBus.register(CreateConceptBankCommand.class, createConceptBankHandler);
        commandBus.register(SignalCommand.class, handleSignalHandler);
    }
}
