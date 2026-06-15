package com.vn.sodu.mail;

import com.vn.sodu.user.Account;

public interface EmailService {
    void send(String to, String subject, String body);
    void sendActivationEmail(Account account, String token);
}