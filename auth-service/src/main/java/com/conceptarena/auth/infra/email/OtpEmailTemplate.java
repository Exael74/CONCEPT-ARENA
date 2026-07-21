package com.conceptarena.auth.infra.email;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Year;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

/**
 * Renders the branded OTP HTML email (email/otp-email.html) by substituting the code, validity and
 * year placeholders. The template is loaded once and cached; used by both email senders so the
 * markup (and the brand palette) live in a single place.
 */
@Component
public class OtpEmailTemplate {

    public static final String SUBJECT = "Tu código de acceso a ConceptArena";

    private final String template;

    public OtpEmailTemplate() {
        try {
            this.template = StreamUtils.copyToString(
                new ClassPathResource("email/otp-email.html").getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load OTP email template", e);
        }
    }

    public String render(String code, Duration validity) {
        return template
            .replace("{{CODE}}", code)
            .replace("{{MINUTES}}", Long.toString(validity.toMinutes()))
            .replace("{{YEAR}}", Integer.toString(Year.now().getValue()));
    }
}
