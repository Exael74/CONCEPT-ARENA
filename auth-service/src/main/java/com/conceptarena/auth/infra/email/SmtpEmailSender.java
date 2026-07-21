package com.conceptarena.auth.infra.email;

import com.conceptarena.auth.app.EmailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Real SMTP {@link EmailSender} (docker profile, app.mail.enabled=true): sends the branded HTML OTP
 * email via Spring's JavaMailSender against the configured SMTP server (MailHog in the compose
 * stack — view sent mail at http://localhost:8025). A send failure is logged and swallowed so a
 * transient mail outage doesn't 500 the request; the user can simply request a new code.
 */
@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class SmtpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final JavaMailSender mailSender;
    private final OtpEmailTemplate template;
    private final String from;

    public SmtpEmailSender(JavaMailSender mailSender, OtpEmailTemplate template,
                           @Value("${app.mail.from:no-reply@conceptarena.app}") String from) {
        this.mailSender = mailSender;
        this.template = template;
        this.from = from;
    }

    @Override
    public void sendOtpEmail(String toEmail, String code, Duration validity) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject(OtpEmailTemplate.SUBJECT);
            helper.setText(template.render(code, validity), true); // true => HTML
            mailSender.send(message);
            log.info("OTP email sent to {} (valid {}m)", toEmail, validity.toMinutes());
        } catch (MessagingException | MailException e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
        }
    }
}
