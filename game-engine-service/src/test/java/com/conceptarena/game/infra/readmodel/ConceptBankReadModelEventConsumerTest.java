package com.conceptarena.game.infra.readmodel;

import static org.assertj.core.api.Assertions.assertThat;

import com.conceptarena.game.infra.readmodel.dto.ConceptBankCreatedMessage;
import com.conceptarena.game.infra.readmodel.dto.ConceptBankCreatedMessage.ConceptSnapshotMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ConceptBankReadModelEventConsumerTest {

    @Autowired private JpaConceptBankReadModelRepository bankRepository;
    @Autowired private JpaConceptReadModelRepository conceptRepository;

    private ConceptBankReadModelEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new ConceptBankReadModelEventConsumer(bankRepository, conceptRepository, new ObjectMapper());
    }

    @Test
    void persistsBankAndAllConceptsOnFirstDelivery() {
        ConceptBankCreatedMessage msg = new ConceptBankCreatedMessage(
            "evt-1", "2026-01-01T00:00:00Z", "bank-1", "ARSW", "Software Architecture",
            List.of(new ConceptSnapshotMessage("q1", "a1", 1), new ConceptSnapshotMessage("q2", "a2", 2)));

        consumer.handleConceptBankCreated(msg);

        assertThat(bankRepository.findById("bank-1")).isPresent();
        assertThat(conceptRepository.findByBankId("bank-1")).hasSize(2);
    }

    @Test
    void isIdempotentUnderRedelivery() {
        ConceptBankCreatedMessage msg = new ConceptBankCreatedMessage(
            "evt-1", "2026-01-01T00:00:00Z", "bank-2", "ARSW", "Software Architecture",
            List.of(new ConceptSnapshotMessage("q1", "a1", 1)));

        consumer.handleConceptBankCreated(msg);
        consumer.handleConceptBankCreated(msg); // redelivery — must not duplicate concepts

        assertThat(conceptRepository.findByBankId("bank-2")).hasSize(1);
    }
}
