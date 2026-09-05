package com.vn.sodu.order;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.global.dto.PageResponse;
import com.vn.sodu.order.controller.OrderSyncController;
import com.vn.sodu.order.dtos.OrderResponseDto;
import com.vn.sodu.order.dtos.OrderSyncResultDto;
import com.vn.sodu.order.services.OrderQueryService;
import com.vn.sodu.order.services.OrderService;
import com.vn.sodu.order.services.OrderSyncService;
import com.vn.sodu.request.OrderType;
import com.vn.sodu.integration.NhanhEnabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSyncControllerTest {

    @Mock
    private OrderSyncService orderSyncService;

    @Mock
    private ObjectProvider<OrderSyncService> orderSyncServiceProvider;

    @Mock
    private OrderQueryService orderQueryService;

    @Mock
    private com.vn.sodu.order.services.OrderExportService orderExportService;

    @Mock
    private NhanhEnabled nhanhEnabled;

    @Mock
    private com.vn.sodu.order.services.OrderService orderService;

    @Mock
    private com.vn.sodu.order.mapper.OrderResponseMapper orderResponseMapper;

    private OrderSyncController createController() {
        return new OrderSyncController(
                syncServiceProvider(),
                orderQueryService,
                orderService,
                orderResponseMapper,
                orderExportService,
                nhanhEnabled
        );
    }

    @Test
    void listOrdersRequiresStaffAndReturnsPage() {
        OrderResponseDto dto = OrderResponseDto.builder()
                .id(1L)
                .orderCode("SOBU-REQ-1")
                .syncStatus(OrderSyncStatus.PENDING)
                .build();
        when(orderQueryService.listOrders(0, 20, "createdAt", "DESC"))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1));
        OrderSyncController controller = createController();

        ResponseEntity<ApiResponseDTO<PageResponse<OrderResponseDto>>> response =
                controller.listOrders(staffAuth(), 0, 20, "createdAt", "DESC");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getTotalElements()).isEqualTo(1);
        assertThat(response.getBody().getData().getContent().get(0).getOrderCode()).isEqualTo("SOBU-REQ-1");
        verify(orderQueryService).listOrders(0, 20, "createdAt", "DESC");
    }

    @Test
    void getOrderDetailRequiresStaffAndReturnsOrder() {
        OrderResponseDto dto = OrderResponseDto.builder()
                .id(1L)
                .orderCode("SOBU-REQ-1")
                .items(Collections.emptyList())
                .build();
        when(orderQueryService.getOrderDetail(1L)).thenReturn(dto);
        OrderSyncController controller = createController();

        ResponseEntity<ApiResponseDTO<OrderResponseDto>> response =
                controller.getOrderDetail(1L, staffAuth());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getOrderCode()).isEqualTo("SOBU-REQ-1");
        verify(orderQueryService).getOrderDetail(1L);
    }

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
        OrderSyncController controller = createController();

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
        OrderSyncController controller = createController();

        assertThrows(AccessDeniedException.class,
                () -> controller.retryOrderSync(1L, new UsernamePasswordAuthenticationToken("user", "n/a")));
    }

    @Test
    void retryOrderSyncRejectsLocalModeBeforeResolvingSyncService() {
        doThrow(new com.vn.sodu.integration.NhanhIntegrationDisabledException())
                .when(nhanhEnabled).requireEnabled();
        OrderSyncController controller = createController();

        assertThrows(
                com.vn.sodu.integration.NhanhIntegrationDisabledException.class,
                () -> controller.retryOrderSync(1L, staffAuth())
        );
        verify(orderSyncServiceProvider, never()).getIfAvailable();
        verify(orderSyncService, never()).retryOrderSync(1L);
    }

    @Test
    void updateOrderStatusRequiresStaffAndReturnsUpdatedDto() {
        Order order = Order.builder()
                .id(1L)
                .status(OrderStatus.SHIPPED)
                .build();
        OrderResponseDto responseDto = OrderResponseDto.builder()
                .id(1L)
                .status(OrderStatus.SHIPPED)
                .trackingCode("TRACK123")
                .build();
        com.vn.sodu.order.dtos.UpdateOrderStatusDto updateDto = com.vn.sodu.order.dtos.UpdateOrderStatusDto.builder()
                .status(OrderStatus.SHIPPED)
                .trackingCode("TRACK123")
                .build();

        when(orderService.updateOrderStatusAsAdmin(eq(1L), any(), any())).thenReturn(order);
        when(orderResponseMapper.toDto(order)).thenReturn(responseDto);

        OrderSyncController controller = createController();
        ResponseEntity<ApiResponseDTO<OrderResponseDto>> response =
                controller.updateOrderStatus(1L, updateDto, staffAuth());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(response.getBody().getData().getTrackingCode()).isEqualTo("TRACK123");
    }

    @Test
    void updateOrderStatusRejectsNonStaff() {
        OrderSyncController controller = createController();
        com.vn.sodu.order.dtos.UpdateOrderStatusDto updateDto = com.vn.sodu.order.dtos.UpdateOrderStatusDto.builder()
                .status(OrderStatus.SHIPPED)
                .build();

        assertThrows(AccessDeniedException.class,
                () -> controller.updateOrderStatus(1L, updateDto, new UsernamePasswordAuthenticationToken("user", "n/a")));
    }

    private ObjectProvider<OrderSyncService> syncServiceProvider() {
        lenient().when(orderSyncServiceProvider.getIfAvailable()).thenReturn(orderSyncService);
        return orderSyncServiceProvider;
    }

    @Test
    void updateOrderStatusPutRequiresStaffAndReturnsUpdatedDto() {
        Order order = Order.builder()
                .id(1L)
                .status(OrderStatus.PROCESSING)
                .build();
        OrderResponseDto responseDto = OrderResponseDto.builder()
                .id(1L)
                .status(OrderStatus.PROCESSING)
                .build();
        com.vn.sodu.order.dtos.UpdateOrderStatusDto updateDto = com.vn.sodu.order.dtos.UpdateOrderStatusDto.builder()
                .status(OrderStatus.PROCESSING)
                .build();

        when(orderService.updateOrderStatusAsAdmin(eq(1L), any(), any())).thenReturn(order);
        when(orderResponseMapper.toDto(order)).thenReturn(responseDto);

        OrderSyncController controller = createController();
        ResponseEntity<ApiResponseDTO<OrderResponseDto>> response =
                controller.updateOrderStatusPut(1L, updateDto, staffAuth());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getStatus()).isEqualTo(OrderStatus.PROCESSING);
        verify(orderService).updateOrderStatusAsAdmin(eq(1L), eq(updateDto), any());
    }

    private Authentication staffAuth() {
        return new UsernamePasswordAuthenticationToken(
                "staff",
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_STAFF"))
        );
    }
}
