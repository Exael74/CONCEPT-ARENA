package com.conceptarena.game.infra.config;

import com.conceptarena.game.app.StartRoundCommandHandler;
import com.conceptarena.game.app.SubmitAnswerCommandHandler;
import com.conceptarena.game.domain.command.StartRoundCommand;
import com.conceptarena.game.domain.command.SubmitAnswerCommand;
import com.conceptarena.game.infra.bus.InMemoryCommandBus;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusConfig {

    private final InMemoryCommandBus commandBus;
    private final StartRoundCommandHandler startRoundHandler;
    private final SubmitAnswerCommandHandler submitAnswerHandler;

    public BusConfig(InMemoryCommandBus commandBus, StartRoundCommandHandler startRoundHandler,
                      SubmitAnswerCommandHandler submitAnswerHandler) {
        this.commandBus = commandBus;
        this.startRoundHandler = startRoundHandler;
        this.submitAnswerHandler = submitAnswerHandler;
    }

    @PostConstruct
    public void registerHandlers() {
        commandBus.register(StartRoundCommand.class, startRoundHandler);
        commandBus.register(SubmitAnswerCommand.class, submitAnswerHandler);
    }
}
