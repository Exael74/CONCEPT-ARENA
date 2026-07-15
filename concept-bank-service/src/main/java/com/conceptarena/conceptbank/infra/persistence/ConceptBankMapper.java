package com.conceptarena.conceptbank.infra.persistence;

import com.conceptarena.conceptbank.domain.Concept;
import com.conceptarena.conceptbank.domain.ConceptBank;
import com.conceptarena.conceptbank.infra.persistence.jpa.ConceptBankEntity;
import com.conceptarena.conceptbank.infra.persistence.jpa.ConceptEntity;
import com.conceptarena.kernel.valueobject.EntityId;
import java.util.List;
import java.util.stream.Collectors;

public class ConceptBankMapper {

    public static ConceptEntity toEntity(Concept domain, String bankId) {
        ConceptEntity entity = new ConceptEntity();
        entity.setBankId(bankId);
        entity.setQuestion(domain.getQuestion());
        entity.setExpectedAnswer(domain.getExpectedAnswer());
        entity.setDifficulty(domain.getDifficulty());
        return entity;
    }

    public static ConceptBankEntity toEntity(ConceptBank domain) {
        ConceptBankEntity entity = new ConceptBankEntity();
        entity.setId(domain.getId().value());
        entity.setName(domain.getName());
        entity.setSubject(domain.getSubject());

        List<ConceptEntity> conceptEntities = domain.getConcepts().stream()
            .map(c -> toEntity(c, domain.getId().value()))
            .collect(Collectors.toList());
        entity.setConcepts(conceptEntities);

        return entity;
    }

    public static ConceptBank toDomain(ConceptBankEntity entity) {
        List<Concept> concepts = entity.getConcepts().stream()
            .map(c -> new Concept(c.getQuestion(), c.getExpectedAnswer(), c.getDifficulty()))
            .collect(Collectors.toList());
        return ConceptBank.restore(EntityId.from(entity.getId()), entity.getName(), entity.getSubject(), concepts);
    }
}
