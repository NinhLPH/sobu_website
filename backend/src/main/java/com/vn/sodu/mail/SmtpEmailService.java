package com.vn.sodu.mail;

import com.vn.sodu.user.Account;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public SmtpEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void send(String to, String subject, String body) {
        SimpleMailMessage msg = new SimpleMailMessage();
        if (fromAddress != null && !fromAddress.isBlank()) {
            msg.setFrom(fromAddress);
        }
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg);
    }

    @Override
    @Async("mailExecutor")
    public void sendActivationEmail(Account account, String token) {
        String link = String.format("%s/activate?token=%s", frontendBaseUrl, token);
        String body = String.format(
                "Hello %s,\n\nPlease activate your account by visiting: %s\n\nIf you didn't register, ignore this email.",
                account.getFullName() != null ? account.getFullName() : account.getEmail(), link);
        try {
            send(account.getEmail(), "Activate your Sobu account", body);
        } catch (RuntimeException e) {
            log.error("Failed to send activation email to {}", account.getEmail(), e);
        }
    }
}
