package com.conceptarena.app.concept;

import com.conceptarena.core.concept.model.ConceptBank;
import java.util.Optional;
import java.util.List;

public interface ConceptBankRepository {
    ConceptBank save(ConceptBank bank);
    Optional<ConceptBank> findById(String id);
    List<ConceptBank> findAll();
}
