package com.conceptarena.auth.app;

import java.time.Duration;

/**
 * Port for sending transactional emails. Two implementations: LoggingEmailSender (default — dev/tests,
 * no SMTP needed) and SmtpEmailSender (docker profile, JavaMailSender against the configured SMTP —
 * MailHog in the compose stack). Keeps the OTP handlers independent of the delivery mechanism.
 */
public interface EmailSender {

    /** Sends the branded HTML one-time-password email to {@code toEmail}. */
    void sendOtpEmail(String toEmail, String code, Duration validity);
}
