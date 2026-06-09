package com.vn.sodu.global.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.global.dto.ApiResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final int MAX_KEY_LENGTH = 120;

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    public static boolean hasKey(String idempotencyKey) {
        return idempotencyKey != null && !idempotencyKey.isBlank();
    }

    public <T> ResponseEntity<ApiResponseDTO<T>> execute(
            IdempotencyScope scope,
            String idempotencyKey,
            Object requestIdentity,
            Class<T> responseDataType,
            String resourceType,
            Function<T, Long> resourceIdExtractor,
            Supplier<ResponseEntity<ApiResponseDTO<T>>> action
    ) {
        if (!hasKey(idempotencyKey)) {
            return action.get();
        }

        String normalizedKey = normalizeKey(idempotencyKey);
        String requestHash = hashRequest(requestIdentity);
        Reservation<T> reservation = transactionTemplate.execute(status ->
                reserve(scope, normalizedKey, requestHash, responseDataType)
        );

        if (reservation == null) {
            throw new IllegalStateException("Unable to reserve idempotency key");
        }
        if (reservation.response() != null) {
            return reservation.response();
        }

        try {
            ResponseEntity<ApiResponseDTO<T>> response = action.get();
            complete(reservation.recordId(), response, resourceType, resourceIdExtractor);
            return response;
        } catch (RuntimeException ex) {
            fail(reservation.recordId(), ex);
            throw ex;
        }
    }

    private <T> Reservation<T> reserve(
            IdempotencyScope scope,
            String normalizedKey,
            String requestHash,
            Class<T> responseDataType
    ) {
        return idempotencyRecordRepository.findForUpdate(scope, normalizedKey)
                .map(record -> handleExistingRecord(record, requestHash, responseDataType))
                .orElseGet(() -> {
                    IdempotencyRecord record = IdempotencyRecord.builder()
                            .scope(scope)
                            .idempotencyKey(normalizedKey)
                            .requestHash(requestHash)
                            .status(IdempotencyStatus.IN_PROGRESS)
                            .expiresAt(LocalDateTime.now().plusHours(24))
                            .build();
                    IdempotencyRecord saved = idempotencyRecordRepository.saveAndFlush(record);
                    return new Reservation<>(saved.getId(), null);
                });
    }

    private <T> Reservation<T> handleExistingRecord(
            IdempotencyRecord record,
            String requestHash,
            Class<T> responseDataType
    ) {
        if (!record.getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException("Idempotency-Key was already used with a different request payload");
        }

        if (record.getStatus() == IdempotencyStatus.COMPLETED) {
            return new Reservation<>(record.getId(), replay(record, responseDataType));
        }
        if (record.getStatus() == IdempotencyStatus.IN_PROGRESS) {
            throw new IdempotencyConflictException("Idempotency-Key is already processing");
        }

        record.setStatus(IdempotencyStatus.IN_PROGRESS);
        record.setLastError(null);
        idempotencyRecordRepository.save(record);
        return new Reservation<>(record.getId(), null);
    }

    private <T> void complete(
            Long recordId,
            ResponseEntity<ApiResponseDTO<T>> response,
            String resourceType,
            Function<T, Long> resourceIdExtractor
    ) {
        transactionTemplate.executeWithoutResult(status -> {
            IdempotencyRecord record = idempotencyRecordRepository.findById(recordId)
                    .orElseThrow(() -> new IllegalStateException("Idempotency record not found: " + recordId));
            ApiResponseDTO<T> body = response.getBody();
            T data = body == null ? null : body.getData();
            record.setStatus(IdempotencyStatus.COMPLETED);
            record.setHttpStatus(response.getStatusCode().value());
            record.setResponsePayload(toJson(body));
            record.setResourceType(resourceType);
            record.setResourceId(data == null ? null : resourceIdExtractor.apply(data));
            record.setLastError(null);
            idempotencyRecordRepository.save(record);
        });
    }

    private void fail(Long recordId, RuntimeException ex) {
        transactionTemplate.executeWithoutResult(status -> {
            IdempotencyRecord record = idempotencyRecordRepository.findById(recordId)
                    .orElseThrow(() -> new IllegalStateException("Idempotency record not found: " + recordId));
            record.setStatus(IdempotencyStatus.FAILED);
            record.setLastError(ex.getMessage());
            idempotencyRecordRepository.save(record);
        });
    }

    private <T> ResponseEntity<ApiResponseDTO<T>> replay(IdempotencyRecord record, Class<T> responseDataType) {
        if (record.getResponsePayload() == null || record.getResponsePayload().isBlank()) {
            throw new IdempotencyConflictException("Idempotency-Key completed without a replayable response");
        }
        try {
            JavaType bodyType = objectMapper.getTypeFactory()
                    .constructParametricType(ApiResponseDTO.class, responseDataType);
            ApiResponseDTO<T> body = objectMapper.readValue(record.getResponsePayload(), bodyType);
            return ResponseEntity.status(record.getHttpStatus()).body(body);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to replay idempotent response", ex);
        }
    }

    private String normalizeKey(String idempotencyKey) {
        String normalized = idempotencyKey.trim();
        if (normalized.isBlank()) {
            throw new IdempotencyConflictException("Idempotency-Key must not be blank");
        }
        if (normalized.length() > MAX_KEY_LENGTH) {
            throw new IdempotencyConflictException("Idempotency-Key must be 120 characters or fewer");
        }
        return normalized;
    }

    private String hashRequest(Object requestIdentity) {
        return sha256(toJson(requestIdentity));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Unable to serialize idempotency payload", ex);
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private record Reservation<T>(Long recordId, ResponseEntity<ApiResponseDTO<T>> response) {
    }
}
