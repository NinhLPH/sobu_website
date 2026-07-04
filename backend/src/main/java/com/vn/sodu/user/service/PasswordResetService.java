package com.vn.sodu.user.service;

import com.vn.sodu.global.exception.BadRequestException;
import com.vn.sodu.global.exception.NotFoundException;
import com.vn.sodu.mail.EmailService;
import com.vn.sodu.user.Account;
import com.vn.sodu.user.AccountRepo;
import com.vn.sodu.user.dto.ForgotPasswordRequest;
import com.vn.sodu.user.dto.ResetPasswordRequest;
import com.vn.sodu.user.service.ResetPasswordProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final ResetPasswordProperties resetPasswordProperties;

    private final AccountRepo accountRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private final ConcurrentHashMap<String, Long> cooldownCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> usedTokenCache = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.MINUTES);
    }

    @PreDestroy
    public void destroy() {
        scheduler.shutdown();
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        cooldownCache.entrySet().removeIf(e -> now - e.getValue() > resetPasswordProperties.getCooldownMs());
        usedTokenCache.entrySet().removeIf(e -> now - e.getValue() > resetPasswordProperties.getTokenExpirationMs());
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(resetPasswordProperties.getSecretKey().getBytes());
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        Long lastSent = cooldownCache.get(email);
        if (lastSent != null && System.currentTimeMillis() - lastSent < resetPasswordProperties.getCooldownMs()) {
            throw new BadRequestException("Please wait before requesting another reset email");
        }

        if (accountRepo.findByEmail(email).isEmpty()) {
            log.info("Password reset requested for non-existent email: {}", email);
            return;
        }

        String jti = UUID.randomUUID().toString();
        Date now = new Date();
        Date expiry = new Date(now.getTime() + resetPasswordProperties.getTokenExpirationMs());

        String token = Jwts.builder()
                .subject(email)
                .id(jti)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();

        String resetLink = resetPasswordProperties.getFrontendResetPasswordUrl() + token;
        String body = "Click the link below to reset your password:\n\n" + resetLink
                + "\n\nThis link expires in 15 minutes.\nIf you did not request this, please ignore this email.";

        emailService.send(email, "Password Reset Request", body);
        cooldownCache.put(email, System.currentTimeMillis());

        log.info("Password reset email sent to: {}", email);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(request.getToken())
                    .getPayload();
        } catch (Exception e) {
            throw new BadRequestException("Invalid or expired reset token");
        }

        String jti = claims.getId();
        if (usedTokenCache.containsKey(jti)) {
            throw new BadRequestException("Token has already been used");
        }

        String email = claims.getSubject();
        Account account = accountRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Account not found"));

        account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        accountRepo.save(account);

        usedTokenCache.put(jti, System.currentTimeMillis());
        log.info("Password reset successful for email: {}", email);
    }
}
