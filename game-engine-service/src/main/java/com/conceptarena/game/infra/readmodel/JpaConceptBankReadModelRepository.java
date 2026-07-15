package com.conceptarena.game.infra.readmodel;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaConceptBankReadModelRepository extends JpaRepository<ConceptBankReadModelEntity, String> {
}
