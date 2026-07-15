package com.conceptarena.infra.persistence.jpa.concept;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataConceptBankRepository extends JpaRepository<ConceptBankEntity, String> {
}
