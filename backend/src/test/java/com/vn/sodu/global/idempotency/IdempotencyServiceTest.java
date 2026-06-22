package com.vn.sodu.global.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.request.dto.RequestResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyRecordRepository repository;

    @Mock
    private PlatformTransactionManager transactionManager;

    private IdempotencyService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new SimpleTransactionStatus());
        service = new IdempotencyService(repository, new TransactionTemplate(transactionManager), objectMapper);
    }

    @Test
    void executeStoresCompletedResponseForNewKey() {
        Map<String, Object> requestIdentity = new LinkedHashMap<>();
        requestIdentity.put("action", "create-request");
        requestIdentity.put("body", "payload");
        IdempotencyRecord reserved = IdempotencyRecord.builder()
                .id(1L)
                .scope(IdempotencyScope.CREATE_REQUEST)
                .idempotencyKey("key-1")
                .status(IdempotencyStatus.IN_PROGRESS)
                .build();
        RequestResponseDto dto = RequestResponseDto.builder()
                .id(11L)
                .requestCode("SOBU-REQ-11")
                .build();
        ResponseEntity<ApiResponseDTO<RequestResponseDto>> created = ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(dto, "Request created", HttpStatus.CREATED.value()));

        when(repository.findForUpdate(IdempotencyScope.CREATE_REQUEST, "key-1")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(IdempotencyRecord.class))).thenReturn(reserved);
        when(repository.findById(1L)).thenReturn(Optional.of(reserved));

        ResponseEntity<ApiResponseDTO<RequestResponseDto>> response = service.execute(
                IdempotencyScope.CREATE_REQUEST,
                "key-1",
                requestIdentity,
                RequestResponseDto.class,
                "REQUEST",
                RequestResponseDto::getId,
                () -> created
        );

        assertThat(response).isSameAs(created);
        assertThat(reserved.getStatus()).isEqualTo(IdempotencyStatus.COMPLETED);
        assertThat(reserved.getHttpStatus()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(reserved.getResourceType()).isEqualTo("REQUEST");
        assertThat(reserved.getResourceId()).isEqualTo(11L);
        assertThat(reserved.getResponsePayload()).contains("SOBU-REQ-11");
    }

    @Test
    void executeReplaysCompletedResponseForSameKeyAndPayload() {
        RequestResponseDto dto = RequestResponseDto.builder()
                .id(11L)
                .requestCode("SOBU-REQ-11")
                .build();
        ApiResponseDTO<RequestResponseDto> body = ApiResponseDTO.success(dto, "Request created", HttpStatus.CREATED.value());
        Map<String, Object> requestIdentity = new LinkedHashMap<>();
        requestIdentity.put("action", "create-request");
        requestIdentity.put("body", "payload");

        String requestHash = hashFor(requestIdentity);
        clearInvocations(repository);
        IdempotencyRecord completed = IdempotencyRecord.builder()
                .id(1L)
                .scope(IdempotencyScope.CREATE_REQUEST)
                .idempotencyKey("key-1")
                .requestHash(requestHash)
                .status(IdempotencyStatus.COMPLETED)
                .httpStatus(HttpStatus.CREATED.value())
                .responsePayload(toJson(body))
                .build();

        when(repository.findForUpdate(IdempotencyScope.CREATE_REQUEST, "key-1")).thenReturn(Optional.of(completed));
        Supplier<ResponseEntity<ApiResponseDTO<RequestResponseDto>>> action = () -> {
            throw new AssertionError("Action should not run for completed idempotency replay");
        };

        ResponseEntity<ApiResponseDTO<RequestResponseDto>> response = service.execute(
                IdempotencyScope.CREATE_REQUEST,
                "key-1",
                requestIdentity,
                RequestResponseDto.class,
                "REQUEST",
                RequestResponseDto::getId,
                action
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getRequestCode()).isEqualTo("SOBU-REQ-11");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void executeRejectsSameKeyWithDifferentPayload() {
        IdempotencyRecord completed = IdempotencyRecord.builder()
                .id(1L)
                .scope(IdempotencyScope.CREATE_REQUEST)
                .idempotencyKey("key-1")
                .requestHash("different-hash")
                .status(IdempotencyStatus.COMPLETED)
                .build();
        Map<String, Object> requestIdentity = new LinkedHashMap<>();
        requestIdentity.put("action", "create-request");
        requestIdentity.put("body", "payload");

        when(repository.findForUpdate(IdempotencyScope.CREATE_REQUEST, "key-1")).thenReturn(Optional.of(completed));

        assertThrows(IdempotencyConflictException.class, () -> service.execute(
                IdempotencyScope.CREATE_REQUEST,
                "key-1",
                requestIdentity,
                RequestResponseDto.class,
                "REQUEST",
                RequestResponseDto::getId,
                () -> ResponseEntity.status(HttpStatus.CREATED).body(null)
        ));
    }

    private String hashFor(Object requestIdentity) {
        IdempotencyRecord capture = IdempotencyRecord.builder()
                .id(99L)
                .status(IdempotencyStatus.IN_PROGRESS)
                .build();
        when(repository.findForUpdate(IdempotencyScope.CREATE_REQUEST, "hash-capture")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(IdempotencyRecord.class))).thenAnswer(invocation -> {
            IdempotencyRecord record = invocation.getArgument(0);
            capture.setRequestHash(record.getRequestHash());
            record.setId(99L);
            return record;
        });
        when(repository.findById(99L)).thenReturn(Optional.of(capture));
        service.execute(
                IdempotencyScope.CREATE_REQUEST,
                "hash-capture",
                requestIdentity,
                RequestResponseDto.class,
                "REQUEST",
                RequestResponseDto::getId,
                () -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDTO.success(null, "ok", HttpStatus.CREATED.value()))
        );
        return capture.getRequestHash();
    }

    private String toJson(Object value) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
