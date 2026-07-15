package com.conceptarena.infra.persistence;

import com.conceptarena.app.concept.ConceptBankQueryService;
import com.conceptarena.core.concept.model.ConceptBank;
import com.conceptarena.infra.persistence.jpa.concept.SpringDataConceptBankRepository;
import com.conceptarena.infra.persistence.mapper.ConceptBankMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

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
