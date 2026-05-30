package com.vn.sodu.order;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.order.controller.OrderController;
import com.vn.sodu.order.dtos.OrderResponseDto;
import com.vn.sodu.order.services.OrderQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderQueryService orderQueryService;

    @Test
    void getMyOrderDetailReturnsAuthenticatedCustomerOrder() {
        Authentication auth = customerAuth();
        OrderResponseDto dto = OrderResponseDto.builder()
                .id(1L)
                .orderCode("SOBU-REQ-1")
                .customerMobile("0900000001")
                .build();
        when(orderQueryService.getMyOrderDetail(1L, auth)).thenReturn(dto);
        OrderController controller = new OrderController(orderQueryService);

        ResponseEntity<ApiResponseDTO<OrderResponseDto>> response = controller.getMyOrderDetail(1L, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getOrderCode()).isEqualTo("SOBU-REQ-1");
        verify(orderQueryService).getMyOrderDetail(1L, auth);
    }

    @Test
    void getMyOrderByNhanhOrderIdReturnsAuthenticatedCustomerOrder() {
        Authentication auth = customerAuth();
        OrderResponseDto dto = OrderResponseDto.builder()
                .id(1L)
                .orderCode("SOBU-REQ-1")
                .nhanhOrderId("NH-123")
                .build();
        when(orderQueryService.getMyOrderDetailByNhanhOrderId("NH-123", auth)).thenReturn(dto);
        OrderController controller = new OrderController(orderQueryService);

        ResponseEntity<ApiResponseDTO<OrderResponseDto>> response =
                controller.getMyOrderByNhanhOrderId("NH-123", auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getNhanhOrderId()).isEqualTo("NH-123");
        verify(orderQueryService).getMyOrderDetailByNhanhOrderId("NH-123", auth);
    }

    private Authentication customerAuth() {
        return new UsernamePasswordAuthenticationToken("customer@example.com", "n/a");
    }
}
