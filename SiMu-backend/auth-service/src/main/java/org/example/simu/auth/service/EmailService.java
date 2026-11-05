package org.example.simu.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends verification and password-reset links.
 * <p>
 * Locally the messages go to MailHog (SMTP :1025, UI :8025). A broken SMTP server must never fail a
 * registration, so sending is best-effort and the link is logged at WARN level.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String publicUrl;
    private final boolean enabled;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                        @Value("${app.mail.from:no-reply@simu.local}") String from,
                        @Value("${app.public-url:http://localhost:5173}") String publicUrl,
                        @Value("${app.mail.enabled:true}") boolean enabled) {
        // no spring.mail.host configured -> no JavaMailSender bean -> mail is simply skipped
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.from = from;
        this.publicUrl = publicUrl;
        this.enabled = enabled;
    }

    public void sendVerification(String to, String token) {
        String link = publicUrl + "/verify-email?token=" + token;
        send(to, "SiMu: confirm your e-mail",
                "Welcome to SiMu!\n\nConfirm your e-mail address: " + link + "\n\nThe link is valid for 24 hours.");
    }

    public void sendPasswordReset(String to, String token) {
        String link = publicUrl + "/reset-password?token=" + token;
        send(to, "SiMu: password reset",
                "A password reset was requested for your account.\n\nSet a new password: " + link
                        + "\n\nThe link is valid for 30 minutes. If it was not you, ignore this letter.");
    }

    private void send(String to, String subject, String body) {
        if (!enabled || mailSender == null) {
            log.info("Mail sending is disabled (no SMTP configured), skipping '{}'", subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Mail '{}' sent to {}", subject, to);
        } catch (RuntimeException e) {
            log.warn("Could not send '{}' to {}: {}", subject, to, e.getMessage());
        }
    }
}
