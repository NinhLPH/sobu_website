package com.vn.sodu.nhanh.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEvent;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEventLog;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEventLogRepository;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEventLogStatus;
import com.vn.sodu.nhanh.webhook.NhanhWebhookHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.vn.sodu.nhanh.webhook.NhanhWebhookEventLogStatus.FAILED;
import static com.vn.sodu.nhanh.webhook.NhanhWebhookEventLogStatus.IGNORED;
import static com.vn.sodu.nhanh.webhook.NhanhWebhookEventLogStatus.PROCESSED;
import static com.vn.sodu.nhanh.webhook.NhanhWebhookEventLogStatus.PROCESSING;
import static com.vn.sodu.nhanh.webhook.NhanhWebhookEventLogStatus.RECEIVED;

@Slf4j
@Service
@RequiredArgsConstructor
public class NhanhWebhookProcessor {

    private static final int MAX_ATTEMPTS = 5;
    private static final int BATCH_SIZE = 20;

    private final NhanhWebhookEventLogRepository repository;
    private final ObjectMapper objectMapper;
    private final List<NhanhWebhookHandler> handlers;

    @Scheduled(fixedDelay = 5_000)
    @Transactional
    public void processPendingEvents() {
        log.debug("NhanhWebhookProcessor scanning for pending events");

        List<NhanhWebhookEventLog> pending = repository
                .findByStatusInAndAttemptCountLessThanOrderByReceivedAtAsc(
                        List.of(RECEIVED, FAILED),
                        MAX_ATTEMPTS,
                        PageRequest.of(0, BATCH_SIZE));

        for (NhanhWebhookEventLog eventLog : pending) {
            processEvent(eventLog);
        }
    }

    private void processEvent(NhanhWebhookEventLog eventLog) {
        eventLog.setStatus(PROCESSING);
        repository.save(eventLog);

        try {
            JsonNode payload = objectMapper.readTree(eventLog.getRawPayload());
            JsonNode data = payload != null ? payload.get("data") : null;
            Optional<NhanhWebhookEvent> event = NhanhWebhookEvent.from(eventLog.getEventName());

            if (event.isEmpty()) {
                eventLog.setStatus(IGNORED);
                eventLog.setLastError(null);
                eventLog.setProcessedAt(LocalDateTime.now());
                repository.save(eventLog);
                return;
            }

            NhanhWebhookHandler handler = findHandler(event.get());

            if (handler == null) {
                log.warn("No handler found for Nhanh webhook event={}", eventLog.getEventName());
                eventLog.setStatus(IGNORED);
                eventLog.setLastError("No handler registered for event: " + eventLog.getEventName());
            } else {
                handler.handle(eventLog, data);
                eventLog.setStatus(PROCESSED);
                eventLog.setLastError(null);
                log.info("Successfully processed Nhanh webhook id={}, event={}",
                        eventLog.getId(), eventLog.getEventName());
            }

            eventLog.setProcessedAt(LocalDateTime.now());
            repository.save(eventLog);

        } catch (Exception ex) {
            eventLog.setAttemptCount(eventLog.getAttemptCount() + 1);
            eventLog.setStatus(FAILED);
            eventLog.setLastError(truncate(ex.getMessage(), 1000));
            eventLog.setProcessedAt(LocalDateTime.now());
            repository.save(eventLog);

            log.warn("Failed to process Nhanh webhook id={}, event={}, attempt={}, error={}",
                    eventLog.getId(), eventLog.getEventName(), eventLog.getAttemptCount(), ex.getMessage());
        }
    }

    private NhanhWebhookHandler findHandler(NhanhWebhookEvent event) {
        return handlers.stream()
                .filter(h -> h.supports(event))
                .findFirst()
                .orElse(null);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
