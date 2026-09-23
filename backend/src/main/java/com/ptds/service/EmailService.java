package com.ptds.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends transactional emails. In local/dev profiles point app.mail.host at a
 * tool like MailHog/Mailpit so nothing actually leaves the network.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    public void sendVerificationEmail(String toEmail, String token) {
        String link = frontendBaseUrl + "/verify-email?token=" + token;
        send(toEmail, "Verify your PTDS account",
                "Welcome to the Private Torrent Distribution System!\n\n" +
                "Please verify your email by visiting:\n" + link +
                "\n\nThis link expires in 24 hours.");
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        String link = frontendBaseUrl + "/reset-password?token=" + token;
        send(toEmail, "Reset your PTDS password",
                "We received a request to reset your password.\n\n" +
                "Reset it here (expires in 30 minutes):\n" + link +
                "\n\nIf you did not request this, you can ignore this email.");
    }

    private void send(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (Exception e) {
            // Never fail the calling transaction because of an SMTP hiccup;
            // log loudly so ops can catch delivery problems.
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
