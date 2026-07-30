package com.molla.service;

import jakarta.mail.MessagingException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Simple email service for sending notifications (e.g. on signup or order creation).
 * This is optional in local dev – if mail properties are not configured, emails will
 * be logged but not actually sent.
 */
@Service
@ConditionalOnProperty(prefix = "spring.mail", name = "host", matchIfMissing = false)
public class MailService{

    private final JavaMailSender mailSender;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendSimpleMail(String to, String subject, String body) throws MessagingException {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}

