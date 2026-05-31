package com.vn.sodu.order;

import com.vn.sodu.order.mapper.RequestToOrderMapper;
import com.vn.sodu.order.repo.OrderRepository;
import com.vn.sodu.order.dtos.CreateNormalOrderDto;
import com.vn.sodu.order.dtos.CreateNormalOrderItemDto;
import com.vn.sodu.order.services.OrderService;
import com.vn.sodu.request.OrderType;
import com.vn.sodu.request.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderConversionPolicy orderConversionPolicy;

    @Mock
    private OrderCustomerResolver orderCustomerResolver;

    @Mock
    private RequestToOrderMapper requestToOrderMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                orderConversionPolicy,
                orderCustomerResolver,
                requestToOrderMapper,
                eventPublisher
        );
    }

    @Test
    void createFromApprovedRequestPublishesOrderCreatedEventAfterSavingOrder() {
        Request request = Request.builder()
                .id(10L)
                .customerPhone("0900000001")
                .type(OrderType.NORMAL)
                .build();
        ResolvedOrderCustomer customer = ResolvedOrderCustomer.builder()
                .fullName("Nguyen Van A")
                .phone("0900000001")
                .build();
        Order mappedOrder = Order.builder()
                .request(request)
                .orderCode("SOBU-REQ-1")
                .type(OrderType.NORMAL)
                .build();
        Order savedOrder = Order.builder()
                .id(99L)
                .request(request)
                .orderCode("SOBU-REQ-1")
                .type(OrderType.NORMAL)
                .build();

        when(orderConversionPolicy.getExistingOrder(request)).thenReturn(Optional.empty());
        when(orderCustomerResolver.resolveByPhone("0900000001")).thenReturn(Optional.of(customer));
        when(requestToOrderMapper.mapToOrder(request, customer)).thenReturn(mappedOrder);
        when(orderRepository.save(mappedOrder)).thenReturn(savedOrder);

        Order result = orderService.createFromApprovedRequest(request);

        assertThat(result).isSameAs(savedOrder);
        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().orderId()).isEqualTo(99L);
        assertThat(eventCaptor.getValue().requestId()).isEqualTo(10L);
    }

    @Test
    void createNormalOrderPersistsOrderAndPublishesSyncEventWithoutRequest() {
        CreateNormalOrderDto dto = CreateNormalOrderDto.builder()
                .customerName("Nguyen Van A")
                .customerMobile("0900000001")
                .customerAddress("1 Nguyen Trai")
                .customerCityName("Ho Chi Minh")
                .items(List.of(CreateNormalOrderItemDto.builder()
                        .nhanhProductId("12345")
                        .name("Product A")
                        .price(new BigDecimal("100000"))
                        .quantity(2)
                        .build()))
                .build();

        when(orderRepository.findByOrderCode(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        when(orderRepository.save(org.mockito.ArgumentMatchers.any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(100L);
            return order;
        });

        Order result = orderService.createNormalOrder(dto);

        assertThat(result.getRequest()).isNull();
        assertThat(result.getType()).isEqualTo(OrderType.NORMAL);
        assertThat(result.getSyncStatus()).isEqualTo(OrderSyncStatus.PENDING);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("200000.00");
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getOrder()).isSameAs(result);

        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().orderId()).isEqualTo(100L);
        assertThat(eventCaptor.getValue().requestId()).isNull();
    }
}
