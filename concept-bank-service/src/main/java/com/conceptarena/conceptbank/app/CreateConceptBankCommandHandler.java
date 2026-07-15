package com.conceptarena.conceptbank.app;

import com.conceptarena.conceptbank.app.bus.CommandHandler;
import com.conceptarena.conceptbank.app.bus.EventBus;
import com.conceptarena.conceptbank.domain.ConceptBank;
import com.conceptarena.conceptbank.domain.command.CreateConceptBankCommand;
import com.conceptarena.conceptbank.domain.event.ConceptBankCreated;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateConceptBankCommandHandler implements CommandHandler<CreateConceptBankCommand, String> {

    private final EventBus eventBus;
    private final ConceptBankRepository conceptBankRepository;

    public CreateConceptBankCommandHandler(EventBus eventBus, ConceptBankRepository conceptBankRepository) {
        this.eventBus = eventBus;
        this.conceptBankRepository = conceptBankRepository;
    }

    /**
     * Transactional so the OutboxWritingEventHandler's insert (triggered synchronously by
     * eventBus.publish below) commits atomically with the aggregate save — see
     * docs/architecture-decisions/ADR-002-outbox-pattern-scheduled-polling.md.
     */
    @Override
    @Transactional
    public String handle(CreateConceptBankCommand command) {
        ConceptBank bank = ConceptBank.create(command.name(), command.subject(), command.concepts());
        conceptBankRepository.save(bank);
        eventBus.publish(new ConceptBankCreated(bank.getId().value(), bank.getName(), bank.getSubject(), bank.getConcepts()));
        return bank.getId().value();
    }
}
