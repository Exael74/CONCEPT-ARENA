package com.conceptarena.game.infra.readmodel;

import com.conceptarena.game.infra.readmodel.dto.ConceptBankCreatedMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes concept-bank-service's enriched ConceptBankCreated (carrying the full concept list —
 * see ADR-004) and maintains the local read-model tables ConceptBankReadModelPort reads from.
 */
@Component
public class ConceptBankReadModelEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ConceptBankReadModelEventConsumer.class);

    private final JpaConceptBankReadModelRepository bankRepository;
    private final JpaConceptReadModelRepository conceptRepository;
    private final ObjectMapper objectMapper;

    public ConceptBankReadModelEventConsumer(JpaConceptBankReadModelRepository bankRepository,
                                              JpaConceptReadModelRepository conceptRepository,
                                              ObjectMapper objectMapper) {
        this.bankRepository = bankRepository;
        this.conceptRepository = conceptRepository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "game-engine.conceptbank.created.readmodel")
    public void onConceptBankCreatedMessage(String rawPayload) throws Exception {
        handleConceptBankCreated(objectMapper.readValue(rawPayload, ConceptBankCreatedMessage.class));
    }

    @Transactional
    public void handleConceptBankCreated(ConceptBankCreatedMessage msg) {
        if (bankRepository.existsById(msg.aggregateId())) {
            // Idempotent: a redelivered ConceptBankCreated must not duplicate concepts.
            log.debug("Read-model: ConceptBankCreated for {} already processed, skipping", msg.aggregateId());
            return;
        }
        ConceptBankReadModelEntity bank = new ConceptBankReadModelEntity();
        bank.setBankId(msg.aggregateId());
        bank.setName(msg.name());
        bank.setSubject(msg.subject());
        bankRepository.save(bank);

        msg.concepts().forEach(c -> {
            ConceptReadModelEntity concept = new ConceptReadModelEntity();
            concept.setBankId(msg.aggregateId());
            concept.setQuestion(c.question());
            concept.setExpectedAnswer(c.expectedAnswer());
            concept.setDifficulty(c.difficulty());
            conceptRepository.save(concept);
        });
        log.debug("Read-model: concept bank {} created with {} concepts", msg.aggregateId(), msg.concepts().size());
    }
}
