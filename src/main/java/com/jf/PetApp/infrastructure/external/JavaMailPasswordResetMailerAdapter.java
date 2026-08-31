package com.jf.PetApp.infrastructure.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.jf.PetApp.application.auth.port.PasswordResetMailerPort;

@Service
public class JavaMailPasswordResetMailerAdapter implements PasswordResetMailerPort {

    private static final Logger log = LoggerFactory.getLogger(JavaMailPasswordResetMailerAdapter.class);

    // Spring Boot's mail autoconfiguration only registers a JavaMailSender bean when
    // spring.mail.host is actually set — with the blank dev default, there is no bean to
    // inject at all. ObjectProvider defers that lookup to send-time instead of failing
    // context startup, so the blank-host dev fallback below is reachable.
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final Environment environment;

    @Value("${app.mail.from:}")
    private String fromAddress;

    public JavaMailPasswordResetMailerAdapter(
        ObjectProvider<JavaMailSender> mailSenderProvider,
        Environment environment
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.environment = environment;
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String rawToken) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            if (environment.acceptsProfiles(Profiles.of("prod"))) {
                // Never log a live reset token in production, even as a fallback — a
                // misconfigured spring.mail.host (the only way this branch is reached in
                // prod) must fail loudly instead of silently leaking the secret to logs.
                log.error(
                    "spring.mail.host is not configured in the prod profile; cannot send " +
                    "password reset email to {}. Refusing to log the reset token.",
                    toEmail
                );
                throw new IllegalStateException("Password reset email delivery is not configured");
            }
            // Dev-friendly fallback, same spirit as DotenvLoader being a no-op when .env is
            // absent: no SMTP configured locally, so the flow stays fully exercisable by
            // reading the code from the log instead of a real inbox.
            log.info("spring.mail.host is not configured; logging password reset token instead of emailing it. to={}, token={}", toEmail, rawToken);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setFrom(fromAddress);
        message.setSubject("Reset your PetApp password");
        message.setText(
                "We received a request to reset your PetApp password.\n\n"
                        + "Your reset code: " + rawToken + "\n\n"
                        + "This code expires in 30 minutes. If you didn't request this, you can safely ignore this email.");
        mailSender.send(message);
    }
}
