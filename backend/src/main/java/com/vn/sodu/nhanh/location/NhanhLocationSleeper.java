package com.vn.sodu.nhanh.location;

import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class NhanhLocationSleeper {

    public void sleep(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return;
        }
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new NhanhLocationSyncInterruptedException("Nhanh location sync interrupted", ex);
        }
    }
}
