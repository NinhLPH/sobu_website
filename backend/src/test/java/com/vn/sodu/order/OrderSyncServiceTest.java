package com.vn.sodu.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.nhanh.dto.NhanhOrderAddRequest;
import com.vn.sodu.nhanh.dto.NhanhOrderAddResult;
import com.vn.sodu.nhanh.dto.NhanhOrderEditRequest;
import com.vn.sodu.nhanh.dto.NhanhOrderEditResult;
import com.vn.sodu.nhanh.service.NhanhService;
import com.vn.sodu.order.nhanh.NhanhOrderGateway;
import com.vn.sodu.order.nhanh.NhanhSyncAttemptRepository;
import com.vn.sodu.order.repo.OrderRepository;
import com.vn.sodu.order.services.OrderSyncService;
import com.vn.sodu.payment.OrderPayment;
import com.vn.sodu.payment.PaymentStatus;
import com.vn.sodu.payment.PaymentType;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private NhanhSyncAttemptRepository nhanhSyncAttemptRepository;

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
        lenient().when(nhanhSyncAttemptRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        lenient().when(nhanhSyncAttemptRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        orderSyncService = new OrderSyncService(
                orderRepository,
                requestRepo,
                nhanhService,
                nhanhOrderGateway,
                nhanhSyncAttemptRepository,
                transactionTemplate,
                new ObjectMapper()
        );
    }

    @Test
    void syncOrderToNhanhMarksOrderAndRequestAsSyncedOnSuccess() {
        Order order = normalOrder();
        OrderPayment fullPayment = addPaidPayment(order, 91L, "SOBU-PAY-91", PaymentType.FULL, "300.00");
        NhanhOrderAddResult result = new NhanhOrderAddResult();
        result.setId(123456L);

        when(orderRepository.findWithItemsAndRequestById(1L)).thenReturn(Optional.of(order));
        when(nhanhService.getValidAccessToken()).thenReturn("token");
        when(nhanhOrderGateway.buildAddRequest(order, fullPayment)).thenReturn(new NhanhOrderAddRequest());
        when(nhanhOrderGateway.createOrder(any(NhanhOrderAddRequest.class), eq("token"))).thenReturn(result);

        orderSyncService.syncOrderToNhanh(1L);

        assertThat(order.getSyncStatus()).isEqualTo(OrderSyncStatus.SYNCED);
        assertThat(order.getNhanhSyncStage()).isEqualTo(NhanhSyncStage.NORMAL_ORDER_CREATED);
        assertThat(order.getNhanhOrderId()).isEqualTo("123456");
        assertThat(order.getNhanhOrderCode()).isEqualTo("SOBU-ORD-1");
        assertThat(order.getSyncError()).isNull();
        assertThat(order.getRequest().getNhanhOrderId()).isEqualTo("123456");
        assertThat(order.getRequest().getNhanhOrderCode()).isEqualTo("SOBU-ORD-1");
        verify(orderRepository).save(order);
        verify(requestRepo).save(order.getRequest());
    }

    @Test
    void syncOrderToNhanhMarksOrderFailedWithoutRollingBackApprovedRequest() {
        Order order = normalOrder();
        OrderPayment fullPayment = addPaidPayment(order, 92L, "SOBU-PAY-92", PaymentType.FULL, "300.00");

        when(orderRepository.findWithItemsAndRequestById(1L)).thenReturn(Optional.of(order));
        when(nhanhService.getValidAccessToken()).thenReturn("token");
        when(nhanhOrderGateway.buildAddRequest(order, fullPayment)).thenReturn(new NhanhOrderAddRequest());
        when(nhanhOrderGateway.createOrder(any(NhanhOrderAddRequest.class), eq("token")))
                .thenThrow(new RuntimeException("Nhanh timeout"));

        orderSyncService.syncOrderToNhanh(1L);

        assertThat(order.getSyncStatus()).isEqualTo(OrderSyncStatus.FAILED);
        assertThat(order.getSyncError()).isEqualTo("Nhanh timeout");
        assertThat(order.getRequest().getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(order.getRequest().getNhanhOrderId()).isNull();
        verify(orderRepository).save(order);
        verify(requestRepo, never()).save(order.getRequest());
    }

    @Test
    void syncOrderToNhanhSyncsPaidPreorderDeposit() {
        Order order = preorderDepositOrder();
        OrderPayment depositPayment = addPaidPayment(order, 93L, "SOBU-PAY-93", PaymentType.DEPOSIT, "300.00");
        NhanhOrderAddResult result = new NhanhOrderAddResult();
        result.setId(333444L);

        when(orderRepository.findWithItemsAndRequestById(1L)).thenReturn(Optional.of(order));
        when(nhanhService.getValidAccessToken()).thenReturn("token");
        when(nhanhOrderGateway.buildAddRequest(order, depositPayment)).thenReturn(new NhanhOrderAddRequest());
        when(nhanhOrderGateway.createOrder(any(NhanhOrderAddRequest.class), eq("token"))).thenReturn(result);

        orderSyncService.syncOrderToNhanh(1L);

        assertThat(order.getSyncStatus()).isEqualTo(OrderSyncStatus.SYNCED);
        assertThat(order.getNhanhSyncStage()).isEqualTo(NhanhSyncStage.PREORDER_DEPOSIT_CREATED);
        assertThat(order.getNhanhOrderId()).isEqualTo("333444");
        verify(orderRepository).save(order);
        verify(requestRepo).save(order.getRequest());
    }

    @Test
    void syncOrderToNhanhUsesEditForPaidPreorderFinalPayment() {
        Order order = preorderFinalOrder();
        order.setNhanhOrderId("654321");
        OrderPayment finalPayment = addPaidPayment(order, 94L, "SOBU-PAY-94", PaymentType.FINAL, "700.00");

        when(orderRepository.findWithItemsAndRequestById(1L)).thenReturn(Optional.of(order));
        when(nhanhService.getValidAccessToken()).thenReturn("token");
        when(nhanhOrderGateway.buildEditRequest(order, finalPayment)).thenReturn(new NhanhOrderEditRequest());
        when(nhanhOrderGateway.editOrder(any(NhanhOrderEditRequest.class), eq("token"))).thenReturn(new NhanhOrderEditResult());

        orderSyncService.syncOrderToNhanh(1L);

        assertThat(order.getSyncStatus()).isEqualTo(OrderSyncStatus.SYNCED);
        assertThat(order.getNhanhSyncStage()).isEqualTo(NhanhSyncStage.PREORDER_FINAL_UPDATED);
        assertThat(order.getNhanhOrderId()).isEqualTo("654321");
        verify(orderRepository).save(order);
        verify(requestRepo, never()).save(order.getRequest());
    }

    @Test
    void syncOrderToNhanhSkipsOrdersWithoutConfirmedPayments() {
        Order order = normalOrder();
        order.setPayments(new ArrayList<>());
        order.setPaidAmount(BigDecimal.ZERO);
        order.setPaymentStatus(PaymentStatus.PENDING);

        when(orderRepository.findWithItemsAndRequestById(1L)).thenReturn(Optional.of(order));

        orderSyncService.syncOrderToNhanh(1L);

        assertThat(order.getSyncStatus()).isEqualTo(OrderSyncStatus.PENDING);
        verify(orderRepository, never()).save(order);
        verifyNoInteractions(nhanhService, nhanhOrderGateway, requestRepo);
    }

    @Test
    void syncOrderToNhanhTreatsDuplicateAsSyncedWithWarning() {
        Order order = normalOrder();
        order.setNhanhOrderId("123456");
        OrderPayment fullPayment = addPaidPayment(order, 95L, "SOBU-PAY-95", PaymentType.FULL, "300.00");
        NhanhOrderAddResult result = NhanhOrderAddResult.duplicate();

        when(orderRepository.findWithItemsAndRequestById(1L)).thenReturn(Optional.of(order));
        when(nhanhService.getValidAccessToken()).thenReturn("token");
        when(nhanhOrderGateway.buildAddRequest(order, fullPayment)).thenReturn(new NhanhOrderAddRequest());
        when(nhanhOrderGateway.createOrder(any(NhanhOrderAddRequest.class), eq("token"))).thenReturn(result);

        orderSyncService.syncOrderToNhanh(1L);

        assertThat(order.getSyncStatus()).isEqualTo(OrderSyncStatus.SYNCED);
        assertThat(order.getNhanhOrderId()).isEqualTo("123456");
        assertThat(order.getNhanhOrderCode()).isEqualTo("SOBU-ORD-1");
        assertThat(order.getSyncError()).contains("duplicate");
        verify(orderRepository).save(order);
        verify(requestRepo).save(order.getRequest());
    }

    private Order normalOrder() {
        Request request = Request.builder()
                .id(10L)
                .status(RequestStatus.APPROVED)
                .build();
        return Order.builder()
                .id(1L)
                .orderCode("SOBU-ORD-1")
                .appOrderId("SOBU-ORD-1")
                .request(request)
                .type(OrderType.NORMAL)
                .status(OrderStatus.NEW)
                .customerName("Nguyen Van A")
                .customerMobile("0900000001")
                .customerAddress("12 Le Loi")
                .customerCityId(79L)
                .customerDistrictId(760L)
                .customerWardId(26734L)
                .carrierId(8L)
                .carrierServiceId(1L)
                .shippingFee(new BigDecimal("35000"))
                .paidAmount(new BigDecimal("300.00"))
                .paymentStatus(PaymentStatus.PAID)
                .syncStatus(OrderSyncStatus.PENDING)
                .payments(new ArrayList<>())
                .build();
    }

    private Order preorderDepositOrder() {
        Order order = normalOrder();
        order.setType(OrderType.PREORDER);
        order.setStatus(OrderStatus.DEPOSIT_PAID);
        order.setDepositAmount(new BigDecimal("300.00"));
        order.setTotalAmount(new BigDecimal("1000.00"));
        order.setRemainingAmount(new BigDecimal("700.00"));
        return order;
    }

    private Order preorderFinalOrder() {
        Order order = preorderDepositOrder();
        order.setStatus(OrderStatus.PROCESSING);
        order.setPaidAmount(new BigDecimal("1000.00"));
        order.setRemainingAmount(BigDecimal.ZERO);
        return order;
    }

    private OrderPayment addPaidPayment(Order order, Long id, String paymentCode, PaymentType type, String amount) {
        OrderPayment payment = OrderPayment.builder()
                .id(id)
                .order(order)
                .paymentCode(paymentCode)
                .type(type)
                .status(PaymentStatus.PAID)
                .amount(new BigDecimal(amount))
                .provider("PAYOS_MOCK")
                .paidAt(LocalDateTime.now())
                .build();
        List<OrderPayment> payments = order.getPayments();
        if (payments == null) {
            payments = new ArrayList<>();
            order.setPayments(payments);
        }
        payments.add(payment);
        return payment;
    }
}
