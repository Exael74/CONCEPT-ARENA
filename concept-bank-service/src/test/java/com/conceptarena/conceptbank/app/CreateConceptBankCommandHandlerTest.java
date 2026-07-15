package com.conceptarena.conceptbank.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.conceptarena.conceptbank.app.bus.EventBus;
import com.conceptarena.conceptbank.domain.Concept;
import com.conceptarena.conceptbank.domain.ConceptBank;
import com.conceptarena.conceptbank.domain.command.CreateConceptBankCommand;
import com.conceptarena.conceptbank.domain.event.ConceptBankCreated;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateConceptBankCommandHandlerTest {

    @Mock private EventBus eventBus;
    @Mock private ConceptBankRepository conceptBankRepository;

    private CreateConceptBankCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CreateConceptBankCommandHandler(eventBus, conceptBankRepository);
    }

    private List<Concept> fiveConcepts() {
        return List.of(
            new Concept("q1", "a1", 1), new Concept("q2", "a2", 1), new Concept("q3", "a3", 1),
            new Concept("q4", "a4", 1), new Concept("q5", "a5", 1));
    }

    @Test
    void createsBankAndPublishesEvent() {
        CreateConceptBankCommand command = new CreateConceptBankCommand("Bank", "ARSW", fiveConcepts());

        String bankId = handler.handle(command);

        assertThat(bankId).isNotBlank();
        ArgumentCaptor<ConceptBank> captor = ArgumentCaptor.forClass(ConceptBank.class);
        verify(conceptBankRepository).save(captor.capture());
        assertThat(captor.getValue().getConceptCount()).isEqualTo(5);
        verify(eventBus).publish(any(ConceptBankCreated.class));
    }

    @Test
    void rejectsBankWithFewerThanFiveConcepts() {
        CreateConceptBankCommand command = new CreateConceptBankCommand("Bank", "ARSW",
            List.of(new Concept("q1", "a1", 1)));

        assertThatThrownBy(() -> handler.handle(command)).isInstanceOf(IllegalArgumentException.class);
    }
}
