package com.conceptarena.game.infra.answer;

import com.conceptarena.game.app.AnswerValidationPort;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Validates a submitted answer against the expected answer using normalized string comparison
 * (case-, accent- and whitespace-insensitive).
 *
 * D5: renamed from {@code RestNlpAdapter} (in package {@code infra.nlp}). The old name was
 * misleading — this makes no REST call and performs no NLP; it is a deterministic normalized
 * string match. A future semantic-similarity backend could implement {@link AnswerValidationPort}
 * without changing this contract, and would be the right place to reintroduce a "REST"/"NLP" name.
 */
@Component
public class NormalizingAnswerValidator implements AnswerValidationPort {

    private static final Logger log = LoggerFactory.getLogger(NormalizingAnswerValidator.class);
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    @Override
    public boolean isCorrect(String answerText, String expectedAnswer) {
        boolean match = normalize(answerText).equals(normalize(expectedAnswer));
        log.info("Answer validation: '{}' vs expected '{}' -> {}", answerText, expectedAnswer,
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
