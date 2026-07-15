package com.conceptarena.core.game.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.conceptarena.core.game.model.Answer;
import com.conceptarena.core.game.model.Round;
import com.conceptarena.core.game.model.RoundStatus;
import com.conceptarena.core.shared.valueobject.EntityId;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ScoringServiceTest {

    @Test
    void incorrectAnswerScoresZeroRegardlessOfTiming() {
        Round round = new Round("room-1", "question", "answer", 3, Duration.ofSeconds(20));
        round.start();
        round.submitAnswer("user-1", "wrong");
        Answer answer = round.getAnswers().get("user-1");
        answer.markIncorrect();

        assertThat(ScoringService.calculateScore(round, answer)).isZero();
    }

    @Test
    void correctAnswerSubmittedImmediatelyGetsBaseScorePlusFullSpeedBonus() {
        Instant startedAt = Instant.parse("2026-01-01T00:00:00Z");
        Round round = Round.restore(EntityId.generate(), "room-1", "question", "answer", 2,
            Duration.ofSeconds(10), RoundStatus.ACTIVE, startedAt, null);
        round.restoreAnswer("user-1", "answer", startedAt, Answer.AnswerResult.CORRECT);
        Answer answer = round.getAnswers().get("user-1");

        // base = difficulty(2) * 10 = 20; full-speed bonus = base * 0.5 = 10 -> 30
        assertThat(ScoringService.calculateScore(round, answer)).isEqualTo(30);
    }

    @Test
    void correctAnswerSubmittedAtDeadlineGetsOnlyBaseScore() {
        Instant startedAt = Instant.parse("2026-01-01T00:00:00Z");
        Round round = Round.restore(EntityId.generate(), "room-1", "question", "answer", 2,
            Duration.ofSeconds(10), RoundStatus.ACTIVE, startedAt, null);
        round.restoreAnswer("user-1", "answer", startedAt.plusSeconds(10), Answer.AnswerResult.CORRECT);
        Answer answer = round.getAnswers().get("user-1");

        assertThat(ScoringService.calculateScore(round, answer)).isEqualTo(20);
    }

    @Test
    void correctAnswerWithNoRoundStartTimeGetsBaseScoreOnly() {
        Round round = Round.restore(EntityId.generate(), "room-1", "question", "answer", 4,
            Duration.ofSeconds(15), RoundStatus.ACTIVE, null, null);
        round.restoreAnswer("user-1", "answer", Instant.now(), Answer.AnswerResult.CORRECT);
        Answer answer = round.getAnswers().get("user-1");

        assertThat(ScoringService.calculateScore(round, answer)).isEqualTo(40);
    }

    @Test
    void correctAnswerWithZeroDurationGetsBaseScoreOnly() {
        Instant startedAt = Instant.now();
        Round round = Round.restore(EntityId.generate(), "room-1", "question", "answer", 5,
            Duration.ZERO, RoundStatus.ACTIVE, startedAt, null);
        round.restoreAnswer("user-1", "answer", startedAt, Answer.AnswerResult.CORRECT);
        Answer answer = round.getAnswers().get("user-1");

        assertThat(ScoringService.calculateScore(round, answer)).isEqualTo(50);
    }
}
