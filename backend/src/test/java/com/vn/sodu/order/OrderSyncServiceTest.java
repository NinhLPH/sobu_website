package com.vn.sodu.order;

import com.vn.sodu.nhanh.dto.NhanhOrderAddResult;
import com.vn.sodu.nhanh.service.NhanhService;
import com.vn.sodu.order.nhanh.NhanhOrderGateway;
import com.vn.sodu.request.OrderType;
import com.vn.sodu.request.Request;
import com.vn.sodu.request.RequestStatus;
import com.vn.sodu.request.repo.RequestRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSyncServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RequestRepo requestRepo;

    @Mock
    private NhanhService nhanhService;

    @Mock
    private NhanhOrderGateway nhanhOrderGateway;

    @Mock
    private TransactionTemplate transactionTemplate;

    private OrderSyncService orderSyncService;

    @BeforeEach
    void setUp() {
        lenient().doAnswer(invocation -> {
            Consumer<?> action = invocation.getArgument(0);
            ((Consumer<Object>) action).accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        orderSyncService = new OrderSyncService(orderRepository, requestRepo, nhanhService, nhanhOrderGateway, transactionTemplate);
    }

    @Test
    void syncOrderToNhanhMarksOrderAndRequestAsSyncedOnSuccess() {
        Order order = order(OrderType.NORMAL);
        NhanhOrderAddResult result = new NhanhOrderAddResult();
        result.setId(123456L);
        when(orderRepository.findWithItemsAndRequestById(1L)).thenReturn(Optional.of(order));
        when(nhanhService.getValidAccessToken()).thenReturn("token");
        when(nhanhOrderGateway.createOrder(order, "token")).thenReturn(result);

        orderSyncService.syncOrderToNhanh(1L);

        assertThat(order.getSyncStatus()).isEqualTo(OrderSyncStatus.SYNCED);
        assertThat(order.getNhanhOrderId()).isEqualTo("123456");
        assertThat(order.getNhanhOrderCode()).isEqualTo("SOBU-REQ-1");
        assertThat(order.getSyncError()).isNull();
        assertThat(order.getRequest().getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(order.getRequest().getNhanhOrderId()).isEqualTo("123456");
        assertThat(order.getRequest().getNhanhOrderCode()).isEqualTo("SOBU-REQ-1");
        verify(orderRepository).save(order);
        verify(requestRepo).save(order.getRequest());
    }

    @Test
    void syncOrderToNhanhMarksOrderFailedWithoutRollingBackApprovedRequest() {
        Order order = order(OrderType.NORMAL);
        when(orderRepository.findWithItemsAndRequestById(1L)).thenReturn(Optional.of(order));
        when(nhanhService.getValidAccessToken()).thenReturn("token");
        when(nhanhOrderGateway.createOrder(order, "token")).thenThrow(new RuntimeException("Nhanh timeout"));

        orderSyncService.syncOrderToNhanh(1L);

        assertThat(order.getSyncStatus()).isEqualTo(OrderSyncStatus.FAILED);
        assertThat(order.getSyncError()).isEqualTo("Nhanh timeout");
        assertThat(order.getRequest().getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(order.getRequest().getNhanhOrderId()).isNull();
        verify(orderRepository).save(order);
        verify(requestRepo, never()).save(order.getRequest());
    }

    @Test
    void syncOrderToNhanhSkipsNonNormalOrders() {
        Order order = order(OrderType.PREORDER);
        when(orderRepository.findWithItemsAndRequestById(1L)).thenReturn(Optional.of(order));

        orderSyncService.syncOrderToNhanh(1L);

        assertThat(order.getSyncStatus()).isEqualTo(OrderSyncStatus.PENDING);
        verify(orderRepository, never()).save(order);
        verifyNoInteractions(nhanhService, nhanhOrderGateway, requestRepo);
    }

    @Test
    void syncOrderToNhanhTreatsDuplicateAsSyncedWithWarning() {
        Order order = order(OrderType.NORMAL);
        NhanhOrderAddResult result = NhanhOrderAddResult.duplicate();
        when(orderRepository.findWithItemsAndRequestById(1L)).thenReturn(Optional.of(order));
        when(nhanhService.getValidAccessToken()).thenReturn("token");
        when(nhanhOrderGateway.createOrder(order, "token")).thenReturn(result);

        orderSyncService.syncOrderToNhanh(1L);

        assertThat(order.getSyncStatus()).isEqualTo(OrderSyncStatus.SYNCED);
        assertThat(order.getNhanhOrderId()).isNull();
        assertThat(order.getNhanhOrderCode()).isEqualTo("SOBU-REQ-1");
        assertThat(order.getSyncError()).contains("duplicate");
        verify(orderRepository).save(order);
        verify(requestRepo).save(order.getRequest());
    }

    private Order order(OrderType type) {
        Request request = Request.builder()
                .id(10L)
                .status(RequestStatus.APPROVED)
                .build();
        return Order.builder()
                .id(1L)
                .orderCode("SOBU-REQ-1")
                .request(request)
                .type(type)
                .syncStatus(OrderSyncStatus.PENDING)
                .build();
    }
}
