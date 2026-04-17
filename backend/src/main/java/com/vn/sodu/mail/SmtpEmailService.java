package com.vn.sodu.mail;

import com.vn.sodu.user.Account;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

@Service
public class SmtpEmailService implements EmailService {

    private JavaMailSender mailSender = new JavaMailSenderImpl() {
        @Override
        public void send(SimpleMailMessage... simpleMessages) {
            // no-op default
        }

        @Override
        public void send(MimeMessage... mimeMessages) {
            // no-op default
        }

        @Override
        public MimeMessage createMimeMessage() {
            return new MimeMessage(Session.getDefaultInstance(new Properties()));
        }
    };

    @Value("${server.base-url:http://localhost:8081}")
    private String baseUrl;

    @Autowired
    public SmtpEmailService() {
        this.mailSender = null;
    }

    public SmtpEmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void send(String to, String subject, String body) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        if (mailSender != null) {
            mailSender.send(msg);
        }
    }

    @Override
    public void sendActivationEmail(Account account, String token) {
        String link = String.format("%s/api/auth/activate?token=%s", baseUrl, token);
        String body = String.format("Hello %s,\n\nPlease activate your account by visiting: %s\n\nIf you didn't register, ignore this email.",
                account.getFullName() != null ? account.getFullName() : account.getEmail(), link);
        send(account.getEmail(), "Activate your ChamViet account", body);
    }
}