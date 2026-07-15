package com.conceptarena.infra.nlp;

import com.conceptarena.app.game.AnswerValidationPort;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Validates a submitted answer against the expected answer using normalized string
 * comparison (case, accents, and whitespace insensitive). A REST call to an external
 * semantic-similarity service (e.g. sentence-transformers) can replace this
 * implementation later without changing the {@link AnswerValidationPort} contract.
 */
@Component
public class RestNlpAdapter implements AnswerValidationPort {

    private static final Logger log = LoggerFactory.getLogger(RestNlpAdapter.class);
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    @Override
    public boolean isCorrect(String answerText, String expectedAnswer) {
        boolean match = normalize(answerText).equals(normalize(expectedAnswer));
        log.info("NLP validation: '{}' vs expected '{}' -> {}", answerText, expectedAnswer,
            match ? "CORRECT" : "INCORRECT");
        return match;
    }

    static String normalize(String input) {
        if (input == null) {
            return "";
        }
        String withoutDiacritics = DIACRITICS.matcher(
            Normalizer.normalize(input, Normalizer.Form.NFD)).replaceAll("");
        return WHITESPACE.matcher(withoutDiacritics.toLowerCase(Locale.ROOT).trim()).replaceAll(" ");
    }
}
