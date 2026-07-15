package com.conceptarena.conceptbank.infra.persistence;

import com.conceptarena.conceptbank.app.ConceptBankRepository;
import com.conceptarena.conceptbank.domain.ConceptBank;
import com.conceptarena.conceptbank.infra.persistence.jpa.SpringDataConceptBankRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * ConceptBankEntity.concepts is a lazy @OneToMany. Spring Data JPA's generated methods
 * each run in their own short-lived transaction, which closes before control returns
 * here — so mapping to the domain model (which iterates that collection) must happen
 * inside an explicit transaction of its own, or it fails with LazyInitializationException.
 */
@Repository
public class ConceptBankRepositoryImpl implements ConceptBankRepository {
    private final SpringDataConceptBankRepository jpaRepository;

    public ConceptBankRepositoryImpl(SpringDataConceptBankRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public ConceptBank save(ConceptBank bank) {
        var entity = ConceptBankMapper.toEntity(bank);
        var saved = jpaRepository.save(entity);
        return ConceptBankMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConceptBank> findById(String id) {
        return jpaRepository.findById(id).map(ConceptBankMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConceptBank> findAll() {
        return jpaRepository.findAll().stream()
            .map(ConceptBankMapper::toDomain)
            .collect(Collectors.toList());
    }
}
