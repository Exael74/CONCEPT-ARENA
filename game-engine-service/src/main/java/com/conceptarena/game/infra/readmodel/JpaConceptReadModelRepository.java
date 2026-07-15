package com.conceptarena.game.infra.readmodel;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaConceptReadModelRepository extends JpaRepository<ConceptReadModelEntity, String> {
    List<ConceptReadModelEntity> findByBankId(String bankId);
}
