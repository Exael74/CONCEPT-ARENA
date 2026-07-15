package com.conceptarena.conceptbank.app;

import com.conceptarena.conceptbank.domain.ConceptBank;
import java.util.List;
import java.util.Optional;

public interface ConceptBankRepository {
    ConceptBank save(ConceptBank bank);
    Optional<ConceptBank> findById(String id);
    List<ConceptBank> findAll();
}
