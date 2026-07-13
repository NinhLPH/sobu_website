package com.vn.sodu.nhanh.service;

import com.vn.sodu.nhanh.NhanhOAuthConnectedEvent;
import com.vn.sodu.nhanh.location.NhanhLocationExtendedLockException;
import com.vn.sodu.nhanh.location.NhanhLocationSnapshotStore;
import com.vn.sodu.nhanh.location.NhanhLocationSyncInterruptedException;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

@Component
@Slf4j
public class NhanhLocationSyncCoordinator {

    private final NhanhLocationSyncService syncService;
    private final NhanhLocationSnapshotStore snapshotStore;
    private final NhanhService nhanhService;
    private final ThreadPoolTaskExecutor executor;
    private final TaskScheduler scheduler;
    private final Object lock = new Object();

    private boolean running;
    private boolean pendingOAuth;
    private boolean shuttingDown;
    private Future<?> activeTask;
    private ScheduledFuture<?> deferredTask;
    private Instant deferredAt;
    private Instant cooldownUntil;

    public NhanhLocationSyncCoordinator(
            NhanhLocationSyncService syncService,
            NhanhLocationSnapshotStore snapshotStore,
            NhanhService nhanhService,
            @Qualifier("locationSyncExecutor") ThreadPoolTaskExecutor executor,
            @Qualifier("locationSyncScheduler") TaskScheduler scheduler) {
        this.syncService = syncService;
        this.snapshotStore = snapshotStore;
        this.nhanhService = nhanhService;
        this.executor = executor;
        this.scheduler = scheduler;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            snapshotStore.isExpiredOrMissing();
            nhanhService.getValidAccessToken();
            submit(Trigger.STARTUP);
        } catch (RuntimeException ex) {
            log.warn("Skipping startup Nhanh location sync preflight: {}", ex.getMessage());
        }
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    public void onOAuthConnected(NhanhOAuthConnectedEvent event) {
        submit(Trigger.OAUTH);
    }

    public void triggerManualSync() {
        nhanhService.getValidAccessToken();
        submit(Trigger.MANUAL);
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void pollExpiredSnapshot() {
        try {
            if (!snapshotStore.isExpiredOrMissing()) {
                return;
            }
            nhanhService.getValidAccessToken();
            submit(Trigger.TTL);
        } catch (RuntimeException ex) {
            log.debug("Skipping TTL Nhanh location sync preflight: {}", ex.getMessage());
        }
    }

    private void submit(Trigger trigger) {
        synchronized (lock) {
            if (shuttingDown) {
                return;
            }
            if (cooldownUntil != null && Instant.now().isBefore(cooldownUntil)) {
                if (trigger == Trigger.OAUTH) {
                    pendingOAuth = true;
                }
                scheduleDeferredLocked(cooldownUntil);
                return;
            }
            if (running) {
                if (trigger == Trigger.OAUTH) {
                    pendingOAuth = true;
                }
                return;
            }
            if (trigger == Trigger.DEFERRED) {
                pendingOAuth = false;
            }
            running = true;
            try {
                activeTask = executor.submit(() -> runSync(trigger));
            } catch (RuntimeException ex) {
                running = false;
                throw ex;
            }
        }
    }

    private void runSync(Trigger trigger) {
        log.info("Starting Nhanh location sync. trigger={}", trigger);
        try {
            syncService.synchronize();
        } catch (NhanhLocationExtendedLockException ex) {
            log.warn("Nhanh location sync deferred until {}", ex.getRetryAt());
            scheduleDeferred(ex.getRetryAt());
        } catch (NhanhLocationSyncInterruptedException ex) {
            log.info("Nhanh location sync stopped: {}", ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Nhanh location sync failed. Existing snapshot is unchanged.", ex);
        } finally {
            boolean rerun;
            synchronized (lock) {
                running = false;
                activeTask = null;
                rerun = pendingOAuth && !shuttingDown;
                pendingOAuth = false;
            }
            if (rerun) {
                submit(Trigger.OAUTH);
            }
        }
    }

    private void scheduleDeferred(Instant retryAt) {
        synchronized (lock) {
            if (shuttingDown) {
                return;
            }
            cooldownUntil = retryAt;
            scheduleDeferredLocked(retryAt);
        }
    }

    private void scheduleDeferredLocked(Instant retryAt) {
        if (deferredTask != null && !deferredTask.isDone()
                && deferredAt != null && !retryAt.isBefore(deferredAt)) {
            return;
        }
        if (deferredTask != null) {
            deferredTask.cancel(false);
        }
        deferredAt = retryAt;
        deferredTask = scheduler.schedule(() -> {
            synchronized (lock) {
                deferredTask = null;
                deferredAt = null;
                cooldownUntil = null;
            }
            submit(Trigger.DEFERRED);
        }, retryAt);
    }

    @PreDestroy
    public void shutdown() {
        synchronized (lock) {
            shuttingDown = true;
            pendingOAuth = false;
            if (deferredTask != null) {
                deferredTask.cancel(false);
            }
            if (activeTask != null) {
                activeTask.cancel(true);
            }
        }
    }

    private enum Trigger {
        STARTUP,
        OAUTH,
        MANUAL,
        TTL,
        DEFERRED
    }
}
