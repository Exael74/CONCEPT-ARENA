package com.conceptarena.conceptbank.infra.persistence;

import com.conceptarena.conceptbank.app.ConceptBankQueryService;
import com.conceptarena.conceptbank.domain.ConceptBank;
import com.conceptarena.conceptbank.infra.persistence.jpa.SpringDataConceptBankRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConceptBankQueryServiceImpl implements ConceptBankQueryService {
    private final SpringDataConceptBankRepository repository;

    public ConceptBankQueryServiceImpl(SpringDataConceptBankRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConceptBank> getAllConceptBanks() {
        return repository.findAll().stream()
            .map(ConceptBankMapper::toDomain)
            .collect(Collectors.toList());
    }
}
