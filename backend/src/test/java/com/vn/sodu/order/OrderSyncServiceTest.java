package com.vn.sodu.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.nhanh.dto.NhanhOrderAddRequest;
import com.vn.sodu.nhanh.dto.NhanhOrderAddResult;
import com.vn.sodu.nhanh.dto.NhanhOrderEditRequest;
import com.vn.sodu.nhanh.dto.NhanhOrderEditResult;
import com.vn.sodu.nhanh.dto.NhanhOrderListItem;
import com.vn.sodu.nhanh.NhanhProperties;
import com.vn.sodu.nhanh.service.NhanhService;
import com.vn.sodu.order.nhanh.NhanhOrderGateway;
import com.vn.sodu.order.nhanh.NhanhSyncAttemptRepository;
import com.vn.sodu.order.repo.OrderRepository;
import com.vn.sodu.order.services.OrderSyncService;
import com.vn.sodu.payment.OrderPayment;
import com.vn.sodu.payment.PaymentMethod;
import com.vn.sodu.payment.PaymentStatus;
import com.vn.sodu.payment.PaymentType;
import com.vn.sodu.payment.repo.OrderPaymentRepository;
import com.vn.sodu.request.OrderType;
import com.vn.sodu.request.Request;
import com.vn.sodu.request.RequestStatus;
import com.vn.sodu.request.repo.RequestRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSyncServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderPaymentRepository orderPaymentRepository;

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
        lenient().doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        }).when(transactionTemplate).execute(any());
        lenient().when(nhanhSyncAttemptRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        lenient().when(nhanhSyncAttemptRepository.findTopByBaseKeyOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());
        lenient().when(nhanhSyncAttemptRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        NhanhProperties nhanhProperties = new NhanhProperties();
        orderSyncService = new OrderSyncService(
                orderRepository,
                orderPaymentRepository,
                requestRepo,
                nhanhProperties,
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
    void syncOrderToNhanhStoresNhanhOrderIdFromTrackingUrlWhenIdFieldIsMissing() {
        Order order = normalOrder();
        OrderPayment fullPayment = addPaidPayment(order, 97L, "SOBU-PAY-97", PaymentType.FULL, "300.00");
        NhanhOrderAddResult result = new NhanhOrderAddResult();
        result.setTrackingUrl("https://track.example/order/987654");

        when(orderRepository.findWithItemsAndRequestById(1L)).thenReturn(Optional.of(order));
        when(nhanhService.getValidAccessToken()).thenReturn("token");
        when(nhanhOrderGateway.buildAddRequest(order, fullPayment)).thenReturn(new NhanhOrderAddRequest());
        when(nhanhOrderGateway.createOrder(any(NhanhOrderAddRequest.class), eq("token"))).thenReturn(result);

        orderSyncService.syncOrderToNhanh(1L);

        assertThat(order.getSyncStatus()).isEqualTo(OrderSyncStatus.SYNCED);
        assertThat(order.getNhanhOrderId()).isEqualTo("987654");
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
        OrderPayment fullPayment = addPaidPayment(order, 95L, "SOBU-PAY-95", PaymentType.FULL, "300.00");
        NhanhOrderAddResult result = NhanhOrderAddResult.duplicate();
        NhanhOrderListItem lookupItem = nhanhOrderListItem(123456L, order.getAppOrderId());

        when(orderRepository.findWithItemsAndRequestById(1L)).thenReturn(Optional.of(order));
        when(nhanhService.getValidAccessToken()).thenReturn("token");
        when(nhanhOrderGateway.buildAddRequest(order, fullPayment)).thenReturn(new NhanhOrderAddRequest());
        when(nhanhOrderGateway.createOrder(any(NhanhOrderAddRequest.class), eq("token"))).thenReturn(result);
        when(nhanhOrderGateway.findOrderByReference(order, "token")).thenReturn(Optional.of(lookupItem));

        orderSyncService.syncOrderToNhanh(1L);

        assertThat(order.getSyncStatus()).isEqualTo(OrderSyncStatus.SYNCED);
        assertThat(order.getNhanhOrderId()).isEqualTo("123456");
        assertThat(order.getNhanhOrderCode()).isEqualTo("SOBU-ORD-1");
        assertThat(order.getSyncError()).contains("resolved automatically");
        verify(orderRepository).save(order);
        verify(requestRepo).save(order.getRequest());
    }

    @Test
    void syncOrderToNhanhAllowsNormalCodPaymentBeforePaid() {
        Order order = normalOrder();
        order.setPaidAmount(BigDecimal.ZERO);
        order.setRemainingAmount(new BigDecimal("335000"));
        order.setPaymentStatus(PaymentStatus.PENDING);
        OrderPayment codPayment = addPayment(order, 96L, "SOBU-PAY-96", PaymentType.FULL, PaymentMethod.COD, PaymentStatus.PENDING, "300.00");
        NhanhOrderAddResult result = new NhanhOrderAddResult();
        result.setId(777888L);

        when(orderRepository.findWithItemsAndRequestById(1L)).thenReturn(Optional.of(order));
        when(nhanhService.getValidAccessToken()).thenReturn("token");
        when(nhanhOrderGateway.buildAddRequest(order, codPayment)).thenReturn(new NhanhOrderAddRequest());
        when(nhanhOrderGateway.createOrder(any(NhanhOrderAddRequest.class), eq("token"))).thenReturn(result);

        orderSyncService.syncOrderToNhanh(1L, "SOBU-PAY-96");

        assertThat(order.getSyncStatus()).isEqualTo(OrderSyncStatus.SYNCED);
        assertThat(order.getNhanhOrderId()).isEqualTo("777888");
        verify(orderRepository).save(order);
    }

    @Test
    void syncOrderToNhanhMarksNeedReconcileWhenNhanhDoesNotReturnValidId() {
        Order order = normalOrder();
        OrderPayment fullPayment = addPaidPayment(order, 102L, "SOBU-PAY-102", PaymentType.FULL, "300.00");
        NhanhOrderAddResult result = new NhanhOrderAddResult();

        when(orderRepository.findWithItemsAndRequestById(1L)).thenReturn(Optional.of(order));
        when(nhanhService.getValidAccessToken()).thenReturn("token");
        when(nhanhOrderGateway.buildAddRequest(order, fullPayment)).thenReturn(new NhanhOrderAddRequest());
        when(nhanhOrderGateway.createOrder(any(NhanhOrderAddRequest.class), eq("token"))).thenReturn(result);
        when(nhanhOrderGateway.findOrderByReference(order, "token")).thenReturn(Optional.empty());

        orderSyncService.syncOrderToNhanh(1L);

        assertThat(order.getSyncStatus()).isEqualTo(OrderSyncStatus.NEED_RECONCILE);
        assertThat(order.getNhanhOrderId()).isNull();
        assertThat(order.getSyncError()).contains("manual reconciliation");
        verify(orderRepository).save(order);
        verify(requestRepo, never()).save(order.getRequest());
    }

    @Test
    void syncOrderToNhanhMarksNeedReconcileWhenNhanhReturnsDuplicateWithoutIdAndLookupFails() {
        Order order = normalOrder();
        OrderPayment fullPayment = addPaidPayment(order, 103L, "SOBU-PAY-103", PaymentType.FULL, "300.00");
        NhanhOrderAddResult result = NhanhOrderAddResult.duplicate();

        when(orderRepository.findWithItemsAndRequestById(1L)).thenReturn(Optional.of(order));
        when(nhanhService.getValidAccessToken()).thenReturn("token");
        when(nhanhOrderGateway.buildAddRequest(order, fullPayment)).thenReturn(new NhanhOrderAddRequest());
        when(nhanhOrderGateway.createOrder(any(NhanhOrderAddRequest.class), eq("token"))).thenReturn(result);
        when(nhanhOrderGateway.findOrderByReference(order, "token")).thenReturn(Optional.empty());

        orderSyncService.syncOrderToNhanh(1L);

        assertThat(order.getSyncStatus()).isEqualTo(OrderSyncStatus.NEED_RECONCILE);
        assertThat(order.getNhanhOrderId()).isNull();
        assertThat(order.getSyncError()).contains("could not be resolved automatically");
        verify(orderRepository).save(order);
    }

    @Test
    void syncOrderToNhanhMarksNeedReconcileWhenLookupStillCannotResolveId() {
        Order order = normalOrder();
        OrderPayment fullPayment = addPaidPayment(order, 104L, "SOBU-PAY-104", PaymentType.FULL, "300.00");
        NhanhOrderAddResult result = new NhanhOrderAddResult();
        result.setTrackingUrl("https://track.nhanh.vn/no-digits");

        when(orderRepository.findWithItemsAndRequestById(1L)).thenReturn(Optional.of(order));
        when(nhanhService.getValidAccessToken()).thenReturn("token");
        when(nhanhOrderGateway.buildAddRequest(order, fullPayment)).thenReturn(new NhanhOrderAddRequest());
        when(nhanhOrderGateway.createOrder(any(NhanhOrderAddRequest.class), eq("token"))).thenReturn(result);
        when(nhanhOrderGateway.findOrderByReference(order, "token")).thenReturn(Optional.empty());

        orderSyncService.syncOrderToNhanh(1L);

        assertThat(order.getSyncStatus()).isEqualTo(OrderSyncStatus.NEED_RECONCILE);
        assertThat(order.getNhanhOrderId()).isNull();
        assertThat(order.getSyncError()).isNotNull()
                .contains("manual reconciliation");
    }

    @Test
    void syncOrderToNhanhDoesNotReplacePaymentsCollectionWhenLoadingOrder() {
        Order order = spy(normalOrder());
        order.setPaidAmount(BigDecimal.ZERO);
        order.setPaymentStatus(PaymentStatus.PENDING);

        when(orderRepository.findWithItemsAndRequestById(1L)).thenReturn(Optional.of(order));

        orderSyncService.syncOrderToNhanh(1L);

        verify(order, never()).setPayments(any());
        verifyNoInteractions(orderPaymentRepository);
    }

    @Test
    void recoverOrderSyncsRetriesEligibleFailedNormalOrder() {
        Order order = normalOrder();
        OrderPayment fullPayment = addPaidPayment(order, 111L, "SOBU-PAY-111", PaymentType.FULL, "300.00");
        NhanhOrderAddResult result = new NhanhOrderAddResult();
        result.setId(222333L);
        order.setSyncStatus(OrderSyncStatus.FAILED);

        when(orderRepository.findBySyncStatusInOrderByUpdatedAtAsc(anyCollection(), any())).thenReturn(List.of(order));
        when(orderRepository.findWithItemsAndRequestById(1L)).thenReturn(Optional.of(order));
        when(nhanhService.getValidAccessToken()).thenReturn("token");
        when(nhanhOrderGateway.buildAddRequest(order, fullPayment)).thenReturn(new NhanhOrderAddRequest());
        when(nhanhOrderGateway.createOrder(any(NhanhOrderAddRequest.class), eq("token"))).thenReturn(result);

        orderSyncService.recoverOrderSyncs();

        assertThat(order.getSyncStatus()).isEqualTo(OrderSyncStatus.SYNCED);
        verify(orderRepository).save(order);
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
        return addPayment(order, id, paymentCode, type, PaymentMethod.ONLINE, PaymentStatus.PAID, amount);
    }

    private OrderPayment addPayment(
            Order order,
            Long id,
            String paymentCode,
            PaymentType type,
            PaymentMethod paymentMethod,
            PaymentStatus paymentStatus,
            String amount
    ) {
        OrderPayment payment = OrderPayment.builder()
                .id(id)
                .order(order)
                .paymentCode(paymentCode)
                .type(type)
                .paymentMethod(paymentMethod)
                .status(paymentStatus)
                .amount(new BigDecimal(amount))
                .provider("PAYOS_MOCK")
                .paidAt(paymentStatus == PaymentStatus.PAID ? LocalDateTime.now() : null)
                .build();
        List<OrderPayment> payments = order.getPayments();
        if (payments == null) {
            payments = new ArrayList<>();
            order.setPayments(payments);
        }
        payments.add(payment);
        return payment;
    }

    private NhanhOrderListItem nhanhOrderListItem(Long id, String appOrderId) {
        NhanhOrderListItem item = new NhanhOrderListItem();
        NhanhOrderListItem.Info info = new NhanhOrderListItem.Info();
        info.setId(id);
        item.setInfo(info);
        NhanhOrderListItem.Channel channel = new NhanhOrderListItem.Channel();
        channel.setAppOrderId(appOrderId);
        item.setChannel(channel);
        return item;
    }
}
