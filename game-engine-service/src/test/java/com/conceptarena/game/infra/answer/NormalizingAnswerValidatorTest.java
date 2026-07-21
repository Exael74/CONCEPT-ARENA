package com.conceptarena.game.infra.answer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NormalizingAnswerValidatorTest {

    private final NormalizingAnswerValidator validator = new NormalizingAnswerValidator();

    @Test
    void exactMatchIsCorrect() {
        assertThat(validator.isCorrect("polymorphism", "polymorphism")).isTrue();
    }

    @Test
    void isCaseInsensitive() {
        assertThat(validator.isCorrect("Polymorphism", "polymorphism")).isTrue();
    }

    @Test
    void ignoresAccentsAndDiacritics() {
        assertThat(validator.isCorrect("herencia multiple", "herencia múltiple")).isTrue();
    }

    @Test
    void ignoresLeadingTrailingAndRepeatedWhitespace() {
        assertThat(validator.isCorrect("  polymorphism   is cool  ", "polymorphism is cool")).isTrue();
    }

    @Test
    void differentAnswerIsIncorrect() {
        assertThat(validator.isCorrect("encapsulation", "polymorphism")).isFalse();
    }

    @Test
    void nullAnswerIsIncorrectRatherThanThrowing() {
        assertThat(validator.isCorrect(null, "polymorphism")).isFalse();
    }
}
