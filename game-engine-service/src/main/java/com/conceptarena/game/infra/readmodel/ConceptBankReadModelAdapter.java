package com.conceptarena.game.infra.readmodel;

import com.conceptarena.game.app.readmodel.ConceptBankReadModelPort;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ConceptBankReadModelAdapter implements ConceptBankReadModelPort {

    private final JpaConceptReadModelRepository conceptRepository;

    public ConceptBankReadModelAdapter(JpaConceptReadModelRepository conceptRepository) {
        this.conceptRepository = conceptRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ConceptSnapshot pickRandomConcept(String conceptBankId) {
        List<ConceptReadModelEntity> concepts = conceptRepository.findByBankId(conceptBankId);
        if (concepts.isEmpty()) {
            throw new IllegalStateException("ConceptBank has no concepts (or is unknown): " + conceptBankId);
        }
        ConceptReadModelEntity c = concepts.get(ThreadLocalRandom.current().nextInt(concepts.size()));
        return new ConceptSnapshot(c.getQuestion(), c.getExpectedAnswer(), c.getDifficulty());
    }
}
