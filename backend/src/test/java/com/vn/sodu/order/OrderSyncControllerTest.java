package com.vn.sodu.order;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.request.OrderType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSyncControllerTest {

    @Mock
    private OrderSyncService orderSyncService;

    @Test
    void retryOrderSyncRequiresStaffAndReturnsSyncResult() {
        Order order = Order.builder()
                .id(1L)
                .orderCode("SOBU-REQ-1")
                .type(OrderType.NORMAL)
                .syncStatus(OrderSyncStatus.SYNCED)
                .nhanhOrderId("123")
                .nhanhOrderCode("SOBU-REQ-1")
                .build();
        when(orderSyncService.retryOrderSync(1L)).thenReturn(order);
        OrderSyncController controller = new OrderSyncController(orderSyncService);

        ResponseEntity<ApiResponseDTO<OrderSyncResultDto>> response =
                controller.retryOrderSync(1L, staffAuth());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getOrderId()).isEqualTo(1L);
        assertThat(response.getBody().getData().getSyncStatus()).isEqualTo(OrderSyncStatus.SYNCED);
        verify(orderSyncService).retryOrderSync(1L);
    }

    @Test
    void retryOrderSyncRejectsNonStaff() {
        OrderSyncController controller = new OrderSyncController(orderSyncService);

        assertThrows(AccessDeniedException.class,
                () -> controller.retryOrderSync(1L, new UsernamePasswordAuthenticationToken("user", "n/a")));
    }

    private Authentication staffAuth() {
        return new UsernamePasswordAuthenticationToken(
                "staff",
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_STAFF"))
        );
    }
}
