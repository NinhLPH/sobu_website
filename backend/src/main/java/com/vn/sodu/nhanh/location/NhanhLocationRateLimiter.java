package com.vn.sodu.nhanh.location;

import com.vn.sodu.nhanh.NhanhProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Deque;

@Component
public class NhanhLocationRateLimiter {

    private final Clock clock;
    private final NhanhLocationSleeper sleeper;
    private final NhanhProperties nhanhProperties;
    private final Deque<Long> attempts = new ArrayDeque<>();
    private long lastAttemptAt = Long.MIN_VALUE;

    @Autowired
    public NhanhLocationRateLimiter(
            NhanhLocationSleeper sleeper,
            NhanhProperties nhanhProperties) {
        this(Clock.system(ZoneId.of("Asia/Ho_Chi_Minh")), sleeper, nhanhProperties);
    }

    NhanhLocationRateLimiter(
            Clock clock,
            NhanhLocationSleeper sleeper,
            NhanhProperties nhanhProperties) {
        this.clock = clock;
        this.sleeper = sleeper;
        this.nhanhProperties = nhanhProperties;
    }

    public synchronized void acquire() {
        NhanhProperties.Location config = nhanhProperties.getLocation();
        long requestIntervalMs = Math.max(0, config.getRequestIntervalMs());
        long rollingWindowMs = Math.max(1, config.getRollingWindowMs());
        int rollingWindowMaxRequests = Math.max(
                1,
                config.getRollingWindowMaxRequests());
        while (true) {
            long now = clock.millis();
            long windowStart = now - rollingWindowMs;
            while (!attempts.isEmpty() && attempts.peekFirst() < windowStart) {
                attempts.removeFirst();
            }

            long intervalWait = lastAttemptAt == Long.MIN_VALUE
                    ? 0
                    : lastAttemptAt + requestIntervalMs - now;
            long windowWait = attempts.size() < rollingWindowMaxRequests
                    ? 0
                    : attempts.peekFirst() + rollingWindowMs + 1 - now;
            long waitMs = Math.max(intervalWait, windowWait);
            if (waitMs <= 0) {
                long permittedAt = clock.millis();
                attempts.addLast(permittedAt);
                lastAttemptAt = permittedAt;
                return;
            }
            sleeper.sleep(Duration.ofMillis(waitMs));
        }
    }

    public synchronized void resetAfterPause() {
        lastAttemptAt = clock.millis();
    }
}
