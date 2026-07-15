package com.conceptarena.game.infra.persistence;

import com.conceptarena.game.domain.Answer;
import com.conceptarena.game.domain.Round;
import com.conceptarena.game.domain.RoundStatus;
import com.conceptarena.game.infra.persistence.jpa.AnswerEntity;
import com.conceptarena.game.infra.persistence.jpa.RoundEntity;
import com.conceptarena.kernel.valueobject.EntityId;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public class RoundMapper {

    public static AnswerEntity toEntity(Answer domain, String roundId) {
        AnswerEntity entity = new AnswerEntity();
        entity.setRoundId(roundId);
        entity.setUserId(domain.getUserId());
        entity.setText(domain.getText());
        entity.setSubmittedAt(domain.getSubmittedAt());
        entity.setResult(domain.getResult().name());
        return entity;
    }

    public static RoundEntity toEntity(Round domain) {
        RoundEntity entity = new RoundEntity();
        entity.setId(domain.getId().value());
        entity.setRoomId(domain.getRoomId());
        entity.setConceptQuestion(domain.getConceptQuestion());
        entity.setExpectedAnswer(domain.getExpectedAnswer());
        entity.setDifficulty(domain.getDifficulty());
        entity.setDurationSeconds(domain.getDuration().getSeconds());
        entity.setStatus(domain.getStatus().name());
        if (domain.getStartedAt() != null) entity.setStartedAt(domain.getStartedAt());
        if (domain.getEndedAt() != null) entity.setEndedAt(domain.getEndedAt());

        List<AnswerEntity> answerEntities = domain.getAnswers().values().stream()
            .map(a -> toEntity(a, domain.getId().value()))
            .collect(Collectors.toList());
        entity.setAnswers(answerEntities);

        return entity;
    }

    public static Round toDomain(RoundEntity entity) {
        Round round = Round.restore(
            EntityId.from(entity.getId()),
            entity.getRoomId(),
            entity.getConceptQuestion(),
            entity.getExpectedAnswer(),
            entity.getDifficulty(),
            Duration.ofSeconds(entity.getDurationSeconds()),
            RoundStatus.valueOf(entity.getStatus()),
            entity.getStartedAt(),
            entity.getEndedAt()
        );
        if (entity.getAnswers() != null) {
            entity.getAnswers().forEach(a -> {
                round.restoreAnswer(a.getUserId(), a.getText(), a.getSubmittedAt(), Answer.AnswerResult.valueOf(a.getResult()));
            });
        }
        return round;
    }
}
