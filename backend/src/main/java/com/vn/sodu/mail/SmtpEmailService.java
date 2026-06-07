package com.vn.sodu.mail;

import com.vn.sodu.user.Account;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${server.base-url:http://localhost:8081}")
    private String baseUrl;

    public SmtpEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void send(String to, String subject, String body) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg);
    }

    @Override
    public void sendActivationEmail(Account account, String token) {
        String link = String.format("%s/api/auth/activate?token=%s", baseUrl, token);
        String body = String.format("Hello %s,\n\nPlease activate your account by visiting: %s\n\nIf you didn't register, ignore this email.",
                account.getFullName() != null ? account.getFullName() : account.getEmail(), link);
        send(account.getEmail(), "Activate your Sobu account", body);
    }
}
