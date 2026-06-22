package com.vn.sodu.request.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.request.OrderType;
import com.vn.sodu.request.Request;
import com.vn.sodu.request.RequestStatus;
import com.vn.sodu.request.dto.RequestResponseDto;
import com.vn.sodu.request.dto.UpdateRequestDto;
import com.vn.sodu.request.mapper.RequestResponseMapper;
import com.vn.sodu.request.service.RequestQueryService;
import com.vn.sodu.request.service.RequestWorkflowService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRequestControllerTest {

    @Mock
    private RequestQueryService requestQueryService;

    @Mock
    private RequestWorkflowService requestWorkflowService;

    @Test
    void updateRequestReturnsAdminAdjustedAmountsAndTimestamp() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "admin@sodu.vn",
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        UpdateRequestDto dto = UpdateRequestDto.builder()
                .customRequirements("Admin adjusted pricing")
                .totalAmount(new BigDecimal("550.00"))
                .depositAmount(new BigDecimal("125.00"))
                .build();

        Request updatedRequest = Request.builder()
                .id(5L)
                .requestCode("SOBU-REQ-0005")
                .customerPhone("0912000002")
                .type(OrderType.PREORDER)
                .status(RequestStatus.SOURCING)
                .totalAmount(new BigDecimal("550.00"))
                .depositAmount(new BigDecimal("125.00"))
                .customRequirements("Admin adjusted pricing")
                .updatedAt(LocalDateTime.of(2026, 6, 5, 11, 5, 0))
                .build();

        when(requestWorkflowService.updateRequestAsAdmin(5L, dto)).thenReturn(updatedRequest);

        AdminRequestController controller = new AdminRequestController(
                requestQueryService,
                requestWorkflowService,
                new RequestResponseMapper()
        );

        ResponseEntity<?> response = controller.updateRequest(5L, dto, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(ApiResponseDTO.class);
        ApiResponseDTO<?> body = (ApiResponseDTO<?>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getData()).isInstanceOf(RequestResponseDto.class);
        RequestResponseDto data = (RequestResponseDto) body.getData();
        assertThat(data.getTotalAmount()).isEqualByComparingTo("550.00");
        assertThat(data.getDepositAmount()).isEqualByComparingTo("125.00");
        assertThat(data.getCustomRequirements()).isEqualTo("Admin adjusted pricing");
        assertThat(data.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 6, 5, 11, 5, 0));
        verify(requestWorkflowService).updateRequestAsAdmin(5L, dto);
    }
}
