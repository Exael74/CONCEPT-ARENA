package com.conceptarena.auth.infra.config;

import com.conceptarena.auth.app.LoginUserCommandHandler;
import com.conceptarena.auth.app.RegisterUserCommandHandler;
import com.conceptarena.auth.domain.command.LoginUserCommand;
import com.conceptarena.auth.domain.command.RegisterUserCommand;
import com.conceptarena.auth.infra.bus.InMemoryCommandBus;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusConfig {

    private final InMemoryCommandBus commandBus;
    private final RegisterUserCommandHandler registerUserHandler;
    private final LoginUserCommandHandler loginUserHandler;

    public BusConfig(InMemoryCommandBus commandBus, RegisterUserCommandHandler registerUserHandler,
                      LoginUserCommandHandler loginUserHandler) {
        this.commandBus = commandBus;
        this.registerUserHandler = registerUserHandler;
        this.loginUserHandler = loginUserHandler;
    }

    @PostConstruct
    public void registerHandlers() {
        commandBus.register(RegisterUserCommand.class, registerUserHandler);
        commandBus.register(LoginUserCommand.class, loginUserHandler);
    }
}
