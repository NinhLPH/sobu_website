package com.vn.sodu.nhanh.location;

import java.time.Instant;

public class NhanhLocationExtendedLockException extends RuntimeException {

    private final Instant retryAt;

    public NhanhLocationExtendedLockException(Instant retryAt) {
        super("Nhanh location API is locked until " + retryAt);
        this.retryAt = retryAt;
    }

    public Instant getRetryAt() {
        return retryAt;
    }
}
