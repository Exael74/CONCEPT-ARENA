package com.conceptarena.auth.infra.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class OtpEmailTemplateTest {

    private final OtpEmailTemplate template = new OtpEmailTemplate();

    @Test
    void rendersCodeValidityAndBrandPalette() {
        String html = template.render("482913", Duration.ofMinutes(5));

        assertThat(html).contains("482913");                 // the code
        assertThat(html).contains("5 minutos");              // validity substituted
        assertThat(html).doesNotContain("{{");               // no leftover placeholders
        // Brand palette present (primary / secondary / tertiary / neutral).
        assertThat(html).contains("#3B82F6").contains("#7C3AED").contains("#10B981").contains("#0F172A");
    }
}
