package com.vn.sodu.order;

import com.vn.sodu.order.mapper.RequestToOrderMapper;
import com.vn.sodu.order.repo.OrderRepository;
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
}
