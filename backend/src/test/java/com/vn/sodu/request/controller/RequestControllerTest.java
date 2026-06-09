package com.vn.sodu.request.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.global.idempotency.IdempotencyScope;
import com.vn.sodu.global.idempotency.IdempotencyService;
import com.vn.sodu.request.OrderType;
import com.vn.sodu.request.Request;
import com.vn.sodu.request.RequestStatus;
import com.vn.sodu.request.dto.CreateRequestDto;
import com.vn.sodu.request.dto.RequestResponseDto;
import com.vn.sodu.request.mapper.RequestResponseMapper;
import com.vn.sodu.request.service.RequestQueryService;
import com.vn.sodu.request.service.RequestWorkflowService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestControllerTest {

    @Mock
    private RequestWorkflowService requestWorkflowService;

    @Mock
    private RequestQueryService requestQueryService;

    @Mock
    private RequestResponseMapper requestResponseMapper;

    @Mock
    private IdempotencyService idempotencyService;

    @Test
    void createRequestReturnsCreatedRequest() {
        CreateRequestDto dto = CreateRequestDto.builder()
                .customerPhone("0900000001")
                .type(OrderType.PREORDER)
                .build();
        Request request = Request.builder()
                .id(11L)
                .requestCode("SOBU-REQ-11")
                .customerPhone("0900000001")
                .type(OrderType.PREORDER)
                .status(RequestStatus.SOURCING)
                .build();
        RequestResponseDto responseDto = RequestResponseDto.builder()
                .id(11L)
                .requestCode("SOBU-REQ-11")
                .build();

        when(requestWorkflowService.createRequest(dto)).thenReturn(request);
        when(requestResponseMapper.toDto(request)).thenReturn(responseDto);
        RequestController controller = new RequestController(
                requestWorkflowService,
                requestQueryService,
                requestResponseMapper,
                idempotencyService
        );

        ResponseEntity<?> response = controller.createRequest(dto, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isInstanceOf(ApiResponseDTO.class);
        ApiResponseDTO<?> body = (ApiResponseDTO<?>) response.getBody();
        assertThat(body.getData()).isEqualTo(responseDto);
        verify(requestWorkflowService).createRequest(dto);
    }

    @Test
    void createRequestUsesIdempotencyKeyWhenProvided() {
        CreateRequestDto dto = CreateRequestDto.builder()
                .customerPhone("0900000001")
                .type(OrderType.PREORDER)
                .build();
        RequestResponseDto replayed = RequestResponseDto.builder()
                .id(11L)
                .requestCode("SOBU-REQ-11")
                .build();
        ResponseEntity<ApiResponseDTO<RequestResponseDto>> idempotentResponse = ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(replayed, "Request created", HttpStatus.CREATED.value()));

        when(idempotencyService.execute(
                eq(IdempotencyScope.CREATE_REQUEST),
                eq("request-key-1"),
                any(),
                eq(RequestResponseDto.class),
                eq("REQUEST"),
                any(),
                any()
        )).thenReturn(idempotentResponse);
        RequestController controller = new RequestController(
                requestWorkflowService,
                requestQueryService,
                requestResponseMapper,
                idempotencyService
        );

        ResponseEntity<?> response = controller.createRequest(dto, "request-key-1");

        assertThat(response).isSameAs(idempotentResponse);
        verify(idempotencyService).execute(
                eq(IdempotencyScope.CREATE_REQUEST),
                eq("request-key-1"),
                any(),
                eq(RequestResponseDto.class),
                eq("REQUEST"),
                any(),
                any(Supplier.class)
        );
    }
}
