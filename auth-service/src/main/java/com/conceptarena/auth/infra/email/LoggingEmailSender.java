package com.conceptarena.auth.infra.email;

import com.conceptarena.auth.app.EmailSender;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default {@link EmailSender}: doesn't send real mail — it logs that an OTP email would be sent (and
 * logs the code itself at DEBUG so a developer can complete the flow locally without an SMTP server).
 * Active unless app.mail.enabled=true. The docker profile uses {@link SmtpEmailSender} instead.
 */
@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void sendOtpEmail(String toEmail, String code, Duration validity) {
        log.info("[DEV EMAIL] OTP email suppressed (app.mail.enabled=false); would send to {} (valid {}m).",
            toEmail, validity.toMinutes());
        // DEBUG only: the code is a secret, so it's never logged at INFO/prod level.
        log.debug("[DEV EMAIL] OTP code for {} is {}", toEmail, code);
    }
}
