package com.vn.sodu.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private final Map<String, Instant> blacklistedTokens = new ConcurrentHashMap<>();

    public void blacklist(String token, Date expiresAt) {
        if (token == null || token.isBlank() || expiresAt == null) {
            return;
        }

        Instant expiry = expiresAt.toInstant();
        if (expiry.isBefore(Instant.now())) {
            blacklistedTokens.remove(token);
            return;
        }

        blacklistedTokens.put(token, expiry);
    }

    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        Instant expiry = blacklistedTokens.get(token);
        if (expiry == null) {
            return false;
        }

        if (expiry.isBefore(Instant.now())) {
            blacklistedTokens.remove(token);
            return false;
        }

        return true;
    }
}
