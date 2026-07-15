package com.conceptarena.infra.nlp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RestNlpAdapterTest {

    private final RestNlpAdapter adapter = new RestNlpAdapter();

    @Test
    void exactMatchIsCorrect() {
        assertThat(adapter.isCorrect("polymorphism", "polymorphism")).isTrue();
    }

    @Test
    void isCaseInsensitive() {
        assertThat(adapter.isCorrect("Polymorphism", "polymorphism")).isTrue();
    }

    @Test
    void ignoresAccentsAndDiacritics() {
        assertThat(adapter.isCorrect("herencia multiple", "herencia múltiple")).isTrue();
    }

    @Test
    void ignoresLeadingTrailingAndRepeatedWhitespace() {
        assertThat(adapter.isCorrect("  polymorphism   is cool  ", "polymorphism is cool")).isTrue();
    }

    @Test
    void differentAnswerIsIncorrect() {
        assertThat(adapter.isCorrect("encapsulation", "polymorphism")).isFalse();
    }

    @Test
    void nullAnswerIsIncorrectRatherThanThrowing() {
        assertThat(adapter.isCorrect(null, "polymorphism")).isFalse();
    }
}
