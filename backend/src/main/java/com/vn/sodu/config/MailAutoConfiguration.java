package com.vn.sodu.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.SimpleMailMessage;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.Session;
import java.util.Properties;

@Configuration
public class MailAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.mail.mock", havingValue = "true")
    @ConditionalOnMissingBean(JavaMailSender.class)
    public JavaMailSender javaMailSender() {
        return new JavaMailSenderImpl() {
            @Override
            public void send(SimpleMailMessage... simpleMessages) {
                // no-op
            }

            @Override
            public void send(MimeMessage... mimeMessages) {
                // no-op
            }

            @Override
            public MimeMessage createMimeMessage() {
                return new MimeMessage(Session.getDefaultInstance(new Properties()));
            }
        };
    }
}
