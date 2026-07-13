package com.vn.sodu.order;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.global.idempotency.IdempotencyScope;
import com.vn.sodu.global.idempotency.IdempotencyService;
import com.vn.sodu.order.dtos.CreateNormalOrderDto;
import com.vn.sodu.order.dtos.CreateNormalOrderItemDto;
import com.vn.sodu.order.controller.OrderController;
import com.vn.sodu.order.dtos.OrderResponseDto;
import com.vn.sodu.order.mapper.OrderResponseMapper;
import com.vn.sodu.order.services.OrderQueryService;
import com.vn.sodu.order.services.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderQueryService orderQueryService;

    @Mock
    private OrderService orderService;

    @Mock
    private OrderResponseMapper orderResponseMapper;

    @Mock
    private IdempotencyService idempotencyService;

    @Test
    void getMyOrderDetailReturnsAuthenticatedCustomerOrder() {
        Authentication auth = customerAuth();
        OrderResponseDto dto = OrderResponseDto.builder()
                .id(1L)
                .orderCode("SOBU-REQ-1")
                .customerMobile("0900000001")
                .build();
        when(orderQueryService.getMyOrderDetail(1L, auth)).thenReturn(dto);
        OrderController controller = new OrderController(orderQueryService, orderService, orderResponseMapper, idempotencyService);

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
        OrderController controller = new OrderController(orderQueryService, orderService, orderResponseMapper, idempotencyService);

        ResponseEntity<ApiResponseDTO<OrderResponseDto>> response =
                controller.getMyOrderByNhanhOrderId("NH-123", auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getNhanhOrderId()).isEqualTo("NH-123");
        verify(orderQueryService).getMyOrderDetailByNhanhOrderId("NH-123", auth);
    }

    @Test
    void cancelMyOrderReturnsCancelledOrder() {
        Authentication auth = customerAuth();
        Order order = Order.builder()
                .id(3L)
                .orderCode("SOBU-ORD-3")
                .status(OrderStatus.CANCELLED)
                .build();
        OrderResponseDto dto = OrderResponseDto.builder()
                .id(3L)
                .orderCode("SOBU-ORD-3")
                .status(OrderStatus.CANCELLED)
                .build();

        when(orderService.cancelMyOrder(3L, auth)).thenReturn(order);
        when(orderQueryService.getMyOrderDetail(3L, auth)).thenReturn(dto);
        OrderController controller = new OrderController(orderQueryService, orderService, orderResponseMapper, idempotencyService);

        ResponseEntity<ApiResponseDTO<OrderResponseDto>> response = controller.cancelMyOrder(3L, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderService).cancelMyOrder(3L, auth);
        verify(orderQueryService).getMyOrderDetail(3L, auth);
    }

    @Test
    void createNormalOrderReturnsCreatedOrder() {
        CreateNormalOrderDto request = CreateNormalOrderDto.builder()
                .customerName("Nguyen Van A")
                .customerMobile("0900000001")
                .items(List.of(CreateNormalOrderItemDto.builder()
                        .nhanhProductId("12345")
                        .name("Product A")
                        .price(new BigDecimal("100000"))
                        .quantity(1)
                        .build()))
                .build();
        Order order = Order.builder()
                .id(2L)
                .orderCode("SOBU-ORD-1")
                .build();
        OrderResponseDto dto = OrderResponseDto.builder()
                .id(2L)
                .orderCode("SOBU-ORD-1")
                .build();

        when(orderService.createNormalOrder(request)).thenReturn(order);
        when(orderResponseMapper.toDto(order)).thenReturn(dto);
        OrderController controller = new OrderController(orderQueryService, orderService, orderResponseMapper, idempotencyService);

        ResponseEntity<ApiResponseDTO<OrderResponseDto>> response = controller.createNormalOrder(request, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getOrderCode()).isEqualTo("SOBU-ORD-1");
        verify(orderService).createNormalOrder(request);
        verify(orderResponseMapper).toDto(order);
    }

    @Test
    void createNormalOrderUsesIdempotencyKeyWhenProvided() {
        CreateNormalOrderDto request = CreateNormalOrderDto.builder()
                .customerName("Nguyen Van A")
                .customerMobile("0900000001")
                .items(List.of(CreateNormalOrderItemDto.builder()
                        .nhanhProductId("12345")
                        .name("Product A")
                        .price(new BigDecimal("100000"))
                        .quantity(1)
                        .build()))
                .build();
        OrderResponseDto replayed = OrderResponseDto.builder()
                .id(2L)
                .orderCode("SOBU-ORD-1")
                .build();
        ResponseEntity<ApiResponseDTO<OrderResponseDto>> idempotentResponse = ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(replayed, "Order created", HttpStatus.CREATED.value()));

        when(idempotencyService.execute(
                eq(IdempotencyScope.CREATE_ORDER),
                eq("order-key-1"),
                any(),
                eq(OrderResponseDto.class),
                eq("ORDER"),
                any(),
                any()
        )).thenReturn(idempotentResponse);
        OrderController controller = new OrderController(orderQueryService, orderService, orderResponseMapper, idempotencyService);

        ResponseEntity<ApiResponseDTO<OrderResponseDto>> response = controller.createNormalOrder(request, "order-key-1");

        assertThat(response).isSameAs(idempotentResponse);
        verify(idempotencyService).execute(
                eq(IdempotencyScope.CREATE_ORDER),
                eq("order-key-1"),
                any(),
                eq(OrderResponseDto.class),
                eq("ORDER"),
                any(),
                any(Supplier.class)
        );
    }

    private Authentication customerAuth() {
        return new UsernamePasswordAuthenticationToken("customer@example.com", "n/a");
    }
}
