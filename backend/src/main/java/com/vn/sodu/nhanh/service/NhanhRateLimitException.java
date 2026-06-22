package com.vn.sodu.nhanh.service;

import com.vn.sodu.global.exception.ExternalServiceException;

import java.time.Instant;

public class NhanhRateLimitException extends ExternalServiceException {

    private final Long lockedSeconds;
    private final Instant unlockedAt;

    public NhanhRateLimitException(String message, Long lockedSeconds, Instant unlockedAt) {
        super(message);
        this.lockedSeconds = lockedSeconds;
        this.unlockedAt = unlockedAt;
    }

    public Long getLockedSeconds() {
        return lockedSeconds;
    }

    public Instant getUnlockedAt() {
        return unlockedAt;
    }
}
