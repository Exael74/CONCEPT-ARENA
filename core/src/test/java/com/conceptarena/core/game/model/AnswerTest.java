package com.conceptarena.core.game.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AnswerTest {

    @Test
    void startsAsPending() {
        Answer answer = new Answer("user-1", "some text");
        assertThat(answer.getResult()).isEqualTo(Answer.AnswerResult.PENDING);
        assertThat(answer.getUserId()).isEqualTo("user-1");
        assertThat(answer.getText()).isEqualTo("some text");
        assertThat(answer.getSubmittedAt()).isNotNull();
    }

    @Test
    void markCorrectSetsResultToCorrect() {
        Answer answer = new Answer("user-1", "some text");
        answer.markCorrect();
        assertThat(answer.getResult()).isEqualTo(Answer.AnswerResult.CORRECT);
    }

    @Test
    void markIncorrectSetsResultToIncorrect() {
        Answer answer = new Answer("user-1", "some text");
        answer.markIncorrect();
        assertThat(answer.getResult()).isEqualTo(Answer.AnswerResult.INCORRECT);
    }

    @Test
    void markTimeoutSetsResultToTimeout() {
        Answer answer = new Answer("user-1", "some text");
        answer.markTimeout();
        assertThat(answer.getResult()).isEqualTo(Answer.AnswerResult.TIMEOUT);
    }
}
