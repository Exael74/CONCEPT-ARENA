package com.conceptarena.conceptbank.infra.config;

import com.conceptarena.conceptbank.app.CreateConceptBankCommandHandler;
import com.conceptarena.conceptbank.domain.command.CreateConceptBankCommand;
import com.conceptarena.conceptbank.infra.bus.InMemoryCommandBus;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusConfig {

    private final InMemoryCommandBus commandBus;
    private final CreateConceptBankCommandHandler createConceptBankHandler;

    public BusConfig(InMemoryCommandBus commandBus, CreateConceptBankCommandHandler createConceptBankHandler) {
        this.commandBus = commandBus;
        this.createConceptBankHandler = createConceptBankHandler;
    }

    @PostConstruct
    public void registerHandlers() {
        commandBus.register(CreateConceptBankCommand.class, createConceptBankHandler);
    }
}
