package com.conceptarena.app.concept;

import com.conceptarena.app.bus.CommandHandler;
import com.conceptarena.app.bus.EventBus;
import com.conceptarena.core.concept.command.CreateConceptBankCommand;
import com.conceptarena.core.concept.event.ConceptBankCreated;
import com.conceptarena.core.concept.model.ConceptBank;
import org.springframework.stereotype.Service;

@Service
public class CreateConceptBankCommandHandler implements CommandHandler<CreateConceptBankCommand, String> {

    private final EventBus eventBus;
    private final ConceptBankRepository conceptBankRepository;

    public CreateConceptBankCommandHandler(EventBus eventBus, ConceptBankRepository conceptBankRepository) {
        this.eventBus = eventBus;
        this.conceptBankRepository = conceptBankRepository;
    }

    @Override
    public String handle(CreateConceptBankCommand command) {
        ConceptBank bank = ConceptBank.create(command.name(), command.subject(), command.concepts());
        conceptBankRepository.save(bank);
        eventBus.publish(new ConceptBankCreated(bank.getId().value(), bank.getName(), bank.getSubject()));
        return bank.getId().value();
    }
}
