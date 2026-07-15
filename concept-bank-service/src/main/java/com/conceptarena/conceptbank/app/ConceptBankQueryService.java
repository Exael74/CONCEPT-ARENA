package com.conceptarena.conceptbank.app;

import com.conceptarena.conceptbank.domain.ConceptBank;
import java.util.List;

public interface ConceptBankQueryService {
    List<ConceptBank> getAllConceptBanks();
}
