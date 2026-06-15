package com.vn.sodu.nhanh.service;

import com.vn.sodu.global.exception.ExternalServiceException;

import java.time.Instant;

public class NhanhApiException extends ExternalServiceException {

    private final Integer httpStatus;
    private final String errorCode;
    private final Instant unlockedAt;
    private final boolean transportFailure;

    public NhanhApiException(
            String message,
            Integer httpStatus,
            String errorCode,
            Instant unlockedAt,
            boolean transportFailure,
            Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.unlockedAt = unlockedAt;
        this.transportFailure = transportFailure;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Instant getUnlockedAt() {
        return unlockedAt;
    }

    public boolean isRateLimited() {
        return "ERR_429".equalsIgnoreCase(errorCode) || Integer.valueOf(429).equals(httpStatus);
    }

    public boolean isRetryable() {
        return transportFailure || (httpStatus != null && httpStatus >= 500);
    }
}
