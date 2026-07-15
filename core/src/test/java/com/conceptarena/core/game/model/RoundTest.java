package com.conceptarena.core.game.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoundTest {

    private Round newRound() {
        return new Round("room-1", "What is polymorphism?", "polymorphism", 2, Duration.ofSeconds(30));
    }

    @Test
    void startsInWaitingStatus() {
        Round round = newRound();
        assertThat(round.getStatus()).isEqualTo(RoundStatus.WAITING);
    }

    @Test
    void startActivatesRoundAndRecordsStartTime() {
        Round round = newRound();
        round.start();
        assertThat(round.getStatus()).isEqualTo(RoundStatus.ACTIVE);
        assertThat(round.getStartedAt()).isNotNull();
    }

    @Test
    void submitAnswerBeforeStartThrows() {
        Round round = newRound();
        assertThatThrownBy(() -> round.submitAnswer("user-1", "polymorphism"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void submitAnswerWhileActiveIsRecorded() {
        Round round = newRound();
        round.start();
        round.submitAnswer("user-1", "polymorphism");
        assertThat(round.getAnswers()).containsKey("user-1");
        assertThat(round.getAnswers().get("user-1").getResult()).isEqualTo(Answer.AnswerResult.PENDING);
    }

    @Test
    void sameUserCannotAnswerTwice() {
        Round round = newRound();
        round.start();
        round.submitAnswer("user-1", "polymorphism");
        assertThatThrownBy(() -> round.submitAnswer("user-1", "another answer"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void submitAnswerAfterDeadlineThrowsEvenIfStatusStillActive() {
        Round round = new Round("room-1", "What is polymorphism?", "polymorphism", 2, Duration.ofMillis(50));
        round.start();
        await(100);

        assertThat(round.getStatus()).isEqualTo(RoundStatus.ACTIVE);
        assertThatThrownBy(() -> round.submitAnswer("user-1", "polymorphism"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("expired");
    }

    @Test
    void submitAnswerAfterEndThrows() {
        Round round = newRound();
        round.start();
        round.end();
        assertThatThrownBy(() -> round.submitAnswer("user-1", "polymorphism"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void endMarksRoundEndedAndRecordsEndTime() {
        Round round = newRound();
        round.start();
        round.end();
        assertThat(round.getStatus()).isEqualTo(RoundStatus.ENDED);
        assertThat(round.getEndedAt()).isNotNull();
    }

    @Test
    void allAnsweredIsTrueOnlyWhenEveryParticipantHasAnswered() {
        Round round = newRound();
        round.start();
        round.submitAnswer("user-1", "polymorphism");

        assertThat(round.allAnswered(List.of("user-1"))).isTrue();
        assertThat(round.allAnswered(List.of("user-1", "user-2"))).isFalse();
    }

    @Test
    void elapsedTimeIsZeroBeforeStart() {
        Round round = newRound();
        assertThat(round.getElapsedTime()).isEqualTo(Duration.ZERO);
    }

    private void await(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
