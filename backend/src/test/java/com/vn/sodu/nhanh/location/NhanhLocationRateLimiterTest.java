package com.vn.sodu.nhanh.location;

import com.vn.sodu.nhanh.NhanhProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NhanhLocationRateLimiterTest {

    @Test
    void enforcesIntervalAndRollingWindowLimit() {
        MutableClock clock = new MutableClock();
        AdvancingSleeper sleeper = new AdvancingSleeper(clock);
        NhanhProperties properties = new NhanhProperties();
        properties.getLocation().setRequestIntervalMs(250);
        properties.getLocation().setRollingWindowMs(30_000);
        properties.getLocation().setRollingWindowMaxRequests(120);
        NhanhLocationRateLimiter limiter =
                new NhanhLocationRateLimiter(clock, sleeper, properties);

        List<Long> attempts = new ArrayList<>();
        for (int i = 0; i < 240; i++) {
            limiter.acquire();
            attempts.add(clock.millis());
        }

        for (int i = 1; i < attempts.size(); i++) {
            assertTrue(attempts.get(i) - attempts.get(i - 1) >= 250);
        }
        for (long end : attempts) {
            long start = end - 30_000;
            long count = attempts.stream()
                    .filter(attempt -> attempt >= start && attempt <= end)
                    .count();
            assertTrue(count <= 120);
        }
    }

    private static class AdvancingSleeper extends NhanhLocationSleeper {
        private final MutableClock clock;

        AdvancingSleeper(MutableClock clock) {
            this.clock = clock;
        }

        @Override
        public void sleep(Duration duration) {
            clock.advance(duration);
        }
    }

    private static class MutableClock extends Clock {
        private Instant now = Instant.EPOCH;

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
