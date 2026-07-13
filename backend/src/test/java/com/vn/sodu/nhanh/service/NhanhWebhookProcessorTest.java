package com.vn.sodu.nhanh.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEvent;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEventLog;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEventLogRepository;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEventLogStatus;
import com.vn.sodu.nhanh.webhook.NhanhWebhookHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.vn.sodu.nhanh.webhook.NhanhWebhookEventLogStatus.FAILED;
import static com.vn.sodu.nhanh.webhook.NhanhWebhookEventLogStatus.IGNORED;
import static com.vn.sodu.nhanh.webhook.NhanhWebhookEventLogStatus.PROCESSED;
import static com.vn.sodu.nhanh.webhook.NhanhWebhookEventLogStatus.PROCESSING;
import static com.vn.sodu.nhanh.webhook.NhanhWebhookEventLogStatus.RECEIVED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NhanhWebhookProcessorTest {

    @Mock
    private NhanhWebhookEventLogRepository repository;

    @Mock
    private NhanhWebhookHandler handler;

    @Captor
    private ArgumentCaptor<NhanhWebhookEventLog> captor;

    private ObjectMapper objectMapper;
    private NhanhWebhookProcessor processor;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void dispatchesToMatchingHandlerAndMarksProcessed() {
        when(handler.supports(NhanhWebhookEvent.ORDER_UPDATE)).thenReturn(true);
        processor = new NhanhWebhookProcessor(repository, objectMapper, List.of(handler));

        NhanhWebhookEventLog eventLog = createEventLog(RECEIVED, "orderUpdate", 0);
        when(repository.findByStatusInAndAttemptCountLessThanOrderByReceivedAtAsc(
                eq(List.of(RECEIVED, FAILED)), anyInt(), any(PageRequest.class)))
                .thenReturn(List.of(eventLog));

        processor.processPendingEvents();

        verify(repository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(1).getStatus()).isEqualTo(PROCESSED);
        assertThat(captor.getAllValues().get(1).getAttemptCount()).isZero();
        assertThat(captor.getAllValues().get(1).getLastError()).isNull();
    }

    @Test
    void marksEventAsFailedWhenHandlerThrows() {
        when(handler.supports(NhanhWebhookEvent.ORDER_UPDATE)).thenReturn(true);
        doThrow(new RuntimeException("Something went wrong"))
                .when(handler).handle(any(), any());
        processor = new NhanhWebhookProcessor(repository, objectMapper, List.of(handler));

        NhanhWebhookEventLog eventLog = createEventLog(RECEIVED, "orderUpdate", 0);
        when(repository.findByStatusInAndAttemptCountLessThanOrderByReceivedAtAsc(
                eq(List.of(RECEIVED, FAILED)), anyInt(), any(PageRequest.class)))
                .thenReturn(List.of(eventLog));

        processor.processPendingEvents();

        verify(repository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(1).getStatus()).isEqualTo(FAILED);
        assertThat(captor.getAllValues().get(1).getAttemptCount()).isOne();
        assertThat(captor.getAllValues().get(1).getLastError()).contains("Something went wrong");
    }

    @Test
    void marksEventAsFailedAndIncrementsAttemptCountOnException() {
        when(handler.supports(NhanhWebhookEvent.ORDER_UPDATE)).thenReturn(true);
        doThrow(new RuntimeException("Handler error"))
                .when(handler).handle(any(), any());
        processor = new NhanhWebhookProcessor(repository, objectMapper, List.of(handler));

        NhanhWebhookEventLog eventLog = createEventLog(RECEIVED, "orderUpdate", 2);
        when(repository.findByStatusInAndAttemptCountLessThanOrderByReceivedAtAsc(
                eq(List.of(RECEIVED, FAILED)), anyInt(), any(PageRequest.class)))
                .thenReturn(List.of(eventLog));

        processor.processPendingEvents();

        verify(repository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(1).getStatus()).isEqualTo(FAILED);
        assertThat(captor.getAllValues().get(1).getAttemptCount()).isEqualTo(3);
    }

    @Test
    void marksEventAsIgnoredWhenNoHandlerFound() {
        when(handler.supports(NhanhWebhookEvent.ORDER_UPDATE)).thenReturn(false);
        processor = new NhanhWebhookProcessor(repository, objectMapper, List.of(handler));

        NhanhWebhookEventLog eventLog = createEventLog(RECEIVED, "orderUpdate", 0);
        when(repository.findByStatusInAndAttemptCountLessThanOrderByReceivedAtAsc(
                eq(List.of(RECEIVED, FAILED)), anyInt(), any(PageRequest.class)))
                .thenReturn(List.of(eventLog));

        processor.processPendingEvents();

        verify(repository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(1).getStatus()).isEqualTo(IGNORED);
    }

    @Test
    void onlyProcessesEventsReceivedOrFailed() {
        processor = new NhanhWebhookProcessor(repository, objectMapper, List.of(handler));

        when(repository.findByStatusInAndAttemptCountLessThanOrderByReceivedAtAsc(
                eq(List.of(RECEIVED, FAILED)), anyInt(), any(PageRequest.class)))
                .thenReturn(List.of());

        processor.processPendingEvents();

        verify(repository, never()).save(any(NhanhWebhookEventLog.class));
    }

    @Test
    void transitionsStatusToProcessingBeforeHandling() {
        AtomicReference<NhanhWebhookEventLogStatus> statusDuringHandle = new AtomicReference<>();

        when(handler.supports(NhanhWebhookEvent.ORDER_UPDATE)).thenReturn(true);
        doAnswer(invocation -> {
            NhanhWebhookEventLog log = invocation.getArgument(0);
            statusDuringHandle.set(log.getStatus());
            return null;
        }).when(handler).handle(any(), any());
        processor = new NhanhWebhookProcessor(repository, objectMapper, List.of(handler));

        NhanhWebhookEventLog eventLog = createEventLog(RECEIVED, "orderUpdate", 0);
        when(repository.findByStatusInAndAttemptCountLessThanOrderByReceivedAtAsc(
                eq(List.of(RECEIVED, FAILED)), anyInt(), any(PageRequest.class)))
                .thenReturn(List.of(eventLog));

        processor.processPendingEvents();

        verify(repository, times(2)).save(captor.capture());
        assertThat(statusDuringHandle.get()).isEqualTo(PROCESSING);
    }

    private NhanhWebhookEventLog createEventLog(NhanhWebhookEventLogStatus status, String eventName, int attemptCount) {
        String rawPayload = "{\"event\":\"" + eventName + "\",\"businessId\":\"10000\",\"data\":{\"id\":\"12345\"}}";
        return NhanhWebhookEventLog.builder()
                .id(1L)
                .eventName(eventName)
                .eventType("ORDER_UPDATE")
                .businessId("10000")
                .externalObjectId("12345")
                .payloadHash("abc123")
                .rawPayload(rawPayload)
                .authorizationPresent(true)
                .status(status)
                .attemptCount(attemptCount)
                .receivedAt(LocalDateTime.now())
                .build();
    }
}
