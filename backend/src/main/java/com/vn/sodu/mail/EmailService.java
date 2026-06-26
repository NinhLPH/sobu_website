package com.vn.sodu.mail;

public interface EmailService {
    void send(String to, String subject, String body);
}