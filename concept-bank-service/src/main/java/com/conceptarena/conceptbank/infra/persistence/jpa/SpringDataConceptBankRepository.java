package com.conceptarena.conceptbank.infra.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataConceptBankRepository extends JpaRepository<ConceptBankEntity, String> {
}
