package com.conceptarena.auth.infra.config;

import com.conceptarena.auth.app.LoginUserCommandHandler;
import com.conceptarena.auth.app.RegisterUserCommandHandler;
import com.conceptarena.auth.app.RequestOtpCommandHandler;
import com.conceptarena.auth.app.UpdateUsernameCommandHandler;
import com.conceptarena.auth.app.VerifyOtpCommandHandler;
import com.conceptarena.auth.domain.command.LoginUserCommand;
import com.conceptarena.auth.domain.command.RegisterUserCommand;
import com.conceptarena.auth.domain.command.RequestOtpCommand;
import com.conceptarena.auth.domain.command.UpdateUsernameCommand;
import com.conceptarena.auth.domain.command.VerifyOtpCommand;
import com.conceptarena.auth.infra.bus.InMemoryCommandBus;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusConfig {

    private final InMemoryCommandBus commandBus;
    private final RegisterUserCommandHandler registerUserHandler;
    private final LoginUserCommandHandler loginUserHandler;
    private final RequestOtpCommandHandler requestOtpHandler;
    private final VerifyOtpCommandHandler verifyOtpHandler;
    private final UpdateUsernameCommandHandler updateUsernameHandler;

    public BusConfig(InMemoryCommandBus commandBus, RegisterUserCommandHandler registerUserHandler,
                      LoginUserCommandHandler loginUserHandler, RequestOtpCommandHandler requestOtpHandler,
                      VerifyOtpCommandHandler verifyOtpHandler, UpdateUsernameCommandHandler updateUsernameHandler) {
        this.commandBus = commandBus;
        this.registerUserHandler = registerUserHandler;
        this.loginUserHandler = loginUserHandler;
        this.requestOtpHandler = requestOtpHandler;
        this.verifyOtpHandler = verifyOtpHandler;
        this.updateUsernameHandler = updateUsernameHandler;
    }

    @PostConstruct
    public void registerHandlers() {
        commandBus.register(RegisterUserCommand.class, registerUserHandler);
        commandBus.register(LoginUserCommand.class, loginUserHandler);
        commandBus.register(RequestOtpCommand.class, requestOtpHandler);
        commandBus.register(VerifyOtpCommand.class, verifyOtpHandler);
        commandBus.register(UpdateUsernameCommand.class, updateUsernameHandler);
    }
}
