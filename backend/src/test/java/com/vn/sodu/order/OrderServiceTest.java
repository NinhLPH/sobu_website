package com.vn.sodu.order;

import com.vn.sodu.global.exception.ForbiddenOperationException;
import com.vn.sodu.audit.AuditAction;
import com.vn.sodu.inventory.InsufficientStockException;
import com.vn.sodu.order.mapper.RequestToOrderMapper;
import com.vn.sodu.order.repo.OrderRepository;
import com.vn.sodu.order.dtos.CreateNormalOrderDto;
import com.vn.sodu.order.dtos.CreateNormalOrderItemDto;
import com.vn.sodu.payment.PaymentMethod;
import com.vn.sodu.order.services.OrderService;
import com.vn.sodu.payment.PaymentType;
import com.vn.sodu.payment.service.PaymentCheckoutCreationException;
import com.vn.sodu.payment.service.PaymentService;
import com.vn.sodu.integration.NhanhEnabled;
import com.vn.sodu.product.Product;
import com.vn.sodu.audit.AuditService;
import com.vn.sodu.request.OrderType;
import com.vn.sodu.request.Request;
import com.vn.sodu.user.Account;
import com.vn.sodu.user.AccountRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
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
    private PaymentService paymentService;

    @Mock
    private AccountRepo accountRepo;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private NhanhEnabled nhanhEnabled;

    @Mock
    private AuditService auditService;

    @Mock
    private com.vn.sodu.location.AddressService addressService;

    @Mock
    private com.vn.sodu.product.repo.ProductRepo productRepo;

    @Mock
    private com.vn.sodu.inventory.InventoryService inventoryService;

    @Mock
    private com.vn.sodu.voucher.service.VoucherService voucherService;

    private com.vn.sodu.order.policy.OrderTransitionPolicy orderTransitionPolicy;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderTransitionPolicy = new com.vn.sodu.order.policy.OrderTransitionPolicy();
        lenient().when(nhanhEnabled.isEnabled()).thenReturn(true);
        lenient().when(addressService.isWardInProvince(any(), any())).thenReturn(true);
        lenient().when(voucherService.applyVouchers(any())).thenAnswer(invocation -> {
            com.vn.sodu.voucher.dto.VoucherApplyRequestDto req = invocation.getArgument(0);
            BigDecimal subtotal = req.getSubtotal() != null ? req.getSubtotal() : BigDecimal.ZERO;
            BigDecimal shippingFee = req.getShippingFee() != null ? req.getShippingFee() : BigDecimal.ZERO;
            return com.vn.sodu.voucher.dto.VoucherApplyResponseDto.builder()
                    .valid(true)
                    .subtotalDiscount(BigDecimal.ZERO)
                    .shippingDiscount(BigDecimal.ZERO)
                    .totalDiscount(BigDecimal.ZERO)
                    .originalSubtotal(subtotal)
                    .originalShippingFee(shippingFee)
                    .finalSubtotal(subtotal)
                    .finalShippingFee(shippingFee)
                    .finalTotal(subtotal.add(shippingFee))
                    .message("OK")
                    .build();
        });
        orderService = new OrderService(
                orderRepository,
                orderConversionPolicy,
                orderCustomerResolver,
                requestToOrderMapper,
                paymentService,
                accountRepo,
                eventPublisher,
                nhanhEnabled,
                auditService,
                addressService,
                productRepo,
                inventoryService,
                voucherService,
                orderTransitionPolicy
        );
    }

    @Test
    void createFromApprovedRequestSavesInternalOrderWithoutPublishingSyncEvent() {
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
        verify(paymentService).initializeOrderPaymentState(mappedOrder);
        verify(orderRepository).save(mappedOrder);
    }

    @Test
    void createNormalOrderPersistsOrderWithoutPublishingSyncEvent() {
        CreateNormalOrderDto dto = CreateNormalOrderDto.builder()
                .customerName("Nguyen Van A")
                .customerMobile("0900000001")
                .customerAddress("1 Nguyen Trai")
                .customerStreet("1 Nguyen Trai")
                .customerCityName("Thành phố Hồ Chí Minh")
                .customerCityId(79L)
                .customerDistrictId(null)
                .customerWardId(27154L)
                .carrierId(10L)
                .carrierServiceId(20L)
                .shippingFee(BigDecimal.ZERO)
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
        verify(paymentService).initializeOrderPaymentState(result);
        verify(orderRepository).save(result);
    }

    @Test
    void createFromApprovedPreorderCreatesDepositPaymentAndWaitingStatus() {
        Request request = Request.builder()
                .id(20L)
                .customerPhone("0900000002")
                .type(OrderType.PREORDER)
                .build();
        ResolvedOrderCustomer customer = ResolvedOrderCustomer.builder()
                .fullName("Tran Thi B")
                .phone("0900000002")
                .build();
        Order mappedOrder = Order.builder()
                .request(request)
                .orderCode("SOBU-REQ-20")
                .type(OrderType.PREORDER)
                .depositAmount(new BigDecimal("300.00"))
                .build();
        Order savedOrder = Order.builder()
                .id(120L)
                .request(request)
                .orderCode("SOBU-REQ-20")
                .type(OrderType.PREORDER)
                .depositAmount(new BigDecimal("300.00"))
                .status(OrderStatus.WAITING_DEPOSIT)
                .build();

        when(orderConversionPolicy.getExistingOrder(request)).thenReturn(Optional.empty());
        when(orderCustomerResolver.resolveByPhone("0900000002")).thenReturn(Optional.of(customer));
        when(requestToOrderMapper.mapToOrder(request, customer)).thenReturn(mappedOrder);
        when(orderRepository.save(mappedOrder)).thenReturn(savedOrder);

        Order result = orderService.createFromApprovedRequest(request);

        assertThat(result).isSameAs(savedOrder);
        assertThat(mappedOrder.getStatus()).isEqualTo(OrderStatus.WAITING_DEPOSIT);
        verify(paymentService).initializeOrderPaymentState(mappedOrder);
        verify(orderRepository).save(mappedOrder);
        verify(paymentService).createPayment(savedOrder, PaymentType.DEPOSIT, PaymentMethod.ONLINE);
    }

    @Test
    void createFromApprovedPreorderContinuesWhenInitialDepositCheckoutFails() {
        Request request = Request.builder()
                .id(21L)
                .customerPhone("0900000003")
                .type(OrderType.PREORDER)
                .build();
        ResolvedOrderCustomer customer = ResolvedOrderCustomer.builder()
                .fullName("Le Thi C")
                .phone("0900000003")
                .build();
        Order mappedOrder = Order.builder()
                .request(request)
                .orderCode("SOBU-REQ-21")
                .type(OrderType.PREORDER)
                .depositAmount(new BigDecimal("300.00"))
                .build();
        Order savedOrder = Order.builder()
                .id(121L)
                .request(request)
                .orderCode("SOBU-REQ-21")
                .type(OrderType.PREORDER)
                .depositAmount(new BigDecimal("300.00"))
                .status(OrderStatus.WAITING_DEPOSIT)
                .build();

        when(orderConversionPolicy.getExistingOrder(request)).thenReturn(Optional.empty());
        when(orderCustomerResolver.resolveByPhone("0900000003")).thenReturn(Optional.of(customer));
        when(requestToOrderMapper.mapToOrder(request, customer)).thenReturn(mappedOrder);
        when(orderRepository.save(mappedOrder)).thenReturn(savedOrder);
        doThrow(new PaymentCheckoutCreationException("PayOS unavailable", new IllegalStateException("PayOS unavailable")))
                .when(paymentService)
                .createPayment(savedOrder, PaymentType.DEPOSIT, PaymentMethod.ONLINE);

        Order result = orderService.createFromApprovedRequest(request);

        assertThat(result).isSameAs(savedOrder);
        verify(paymentService).createPayment(savedOrder, PaymentType.DEPOSIT, PaymentMethod.ONLINE);
    }

    @Test
    void cancelMyOrderCancelsOrderBeforeShipping() {
        Authentication auth = customerAuth();
        Account account = new Account();
        account.setEmail("customer@example.com");
        Order order = Order.builder()
                .id(200L)
                .customerEmail("customer@example.com")
                .status(OrderStatus.PROCESSING)
                .build();

        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        when(orderRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order result = orderService.cancelMyOrder(200L, auth);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(order);
        ArgumentCaptor<OrderCancelledEvent> eventCaptor = ArgumentCaptor.forClass(OrderCancelledEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().orderId()).isEqualTo(200L);
    }

    @Test
    void cancelMyOrderDoesNotPublishCancelEventWhenNhanhDisabled() {
        Authentication auth = customerAuth();
        Account account = new Account();
        account.setEmail("customer@example.com");
        Order order = Order.builder()
                .id(201L)
                .customerEmail("customer@example.com")
                .status(OrderStatus.PROCESSING)
                .build();

        when(nhanhEnabled.isEnabled()).thenReturn(false);
        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        when(orderRepository.findByIdForUpdate(201L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order result = orderService.cancelMyOrder(201L, auth);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(order);
        verify(eventPublisher, never()).publishEvent(any());
        verify(auditService).record(
                eq(AuditAction.ORDER_STATUS_OVERRIDE),
                eq("ORDER"),
                eq("201"),
                eq(OrderStatus.PROCESSING.name()),
                eq(OrderStatus.CANCELLED.name()),
                anyString()
        );
    }

    @Test
    void cancelMyOrderRejectsShippedOrder() {
        Authentication auth = customerAuth();
        Account account = new Account();
        account.setEmail("customer@example.com");
        Order order = Order.builder()
                .id(201L)
                .customerEmail("customer@example.com")
                .status(OrderStatus.SHIPPED)
                .build();

        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        when(orderRepository.findByIdForUpdate(201L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelMyOrder(201L, auth))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        verify(orderRepository, never()).save(order);
    }

    @Test
    void createNormalOrderResolvesProductSnapshotsAndReservesStock() {
        Product product = Product.builder()
                .id(500L)
                .name("Snapshot Name")
                .retailPrice(new BigDecimal("200000"))
                .build();
        when(productRepo.findById(500L)).thenReturn(Optional.of(product));

        CreateNormalOrderDto dto = CreateNormalOrderDto.builder()
                .customerName("Nguyen Van A")
                .customerMobile("0900000001")
                .customerStreet("1 Nguyen Trai")
                .customerCityName("Thành phố Hồ Chí Minh")
                .customerCityId(79L)
                .customerDistrictId(null)
                .customerWardId(27154L)
                .carrierId(10L)
                .carrierServiceId(20L)
                .shippingFee(BigDecimal.ZERO)
                .items(List.of(CreateNormalOrderItemDto.builder()
                        .productId(500L)
                        .name("Client-Provided Name")
                        .price(new BigDecimal("100000"))
                        .quantity(2)
                        .build()))
                .build();
        when(orderRepository.findByOrderCode(anyString())).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(100L);
            return order;
        });
        when(inventoryService.reserveForOrder(any(Order.class))).thenReturn(List.of());

        Order result = orderService.createNormalOrder(dto);

        OrderItem item = result.getItems().get(0);
        assertThat(item.getProductId()).isEqualTo(500L);
        assertThat(item.getName()).isEqualTo("Snapshot Name");
        assertThat(item.getPrice()).isEqualByComparingTo("200000.00");
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("400000.00");
        verify(inventoryService).reserveForOrder(result);
    }

    @Test
    void createNormalOrderIgnoresClientProvidedDiscount() {
        Product product = Product.builder()
                .id(500L)
                .name("Snapshot Name")
                .retailPrice(new BigDecimal("100000"))
                .build();
        when(productRepo.findById(500L)).thenReturn(Optional.of(product));

        CreateNormalOrderDto dto = CreateNormalOrderDto.builder()
                .customerName("Nguyen Van A")
                .customerMobile("0900000001")
                .customerStreet("1 Nguyen Trai")
                .customerCityName("Thành phố Hồ Chí Minh")
                .customerCityId(79L)
                .customerDistrictId(null)
                .customerWardId(27154L)
                .carrierId(10L)
                .carrierServiceId(20L)
                .shippingFee(BigDecimal.ZERO)
                .items(List.of(CreateNormalOrderItemDto.builder()
                        .productId(500L)
                        .name("Client-Provided Name")
                        .price(new BigDecimal("100000"))
                        .discount(new BigDecimal("100000"))
                        .quantity(1)
                        .build()))
                .build();
        when(orderRepository.findByOrderCode(anyString())).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(100L);
            return order;
        });
        when(inventoryService.reserveForOrder(any(Order.class))).thenReturn(List.of());

        Order result = orderService.createNormalOrder(dto);

        OrderItem item = result.getItems().get(0);
        assertThat(item.getDiscount()).isEqualByComparingTo("0.00");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("100000.00");
    }

    @Test
    void createNormalOrderRejectsWhenStockIsInsufficient() {
        Product product = Product.builder()
                .id(501L)
                .name("Snapshot Name")
                .retailPrice(new BigDecimal("100000"))
                .build();
        when(productRepo.findById(501L)).thenReturn(Optional.of(product));

        CreateNormalOrderDto dto = CreateNormalOrderDto.builder()
                .customerName("Nguyen Van A")
                .customerMobile("0900000001")
                .customerStreet("1 Nguyen Trai")
                .customerCityName("Thành phố Hồ Chí Minh")
                .customerCityId(79L)
                .customerDistrictId(null)
                .customerWardId(27154L)
                .carrierId(10L)
                .carrierServiceId(20L)
                .shippingFee(BigDecimal.ZERO)
                .items(List.of(CreateNormalOrderItemDto.builder()
                        .productId(501L)
                        .name("Snapshot Name")
                        .quantity(2)
                        .build()))
                .build();
        when(orderRepository.findByOrderCode(anyString())).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryService.reserveForOrder(any(Order.class)))
                .thenThrow(new InsufficientStockException(501L, 2, 1));

        assertThatThrownBy(() -> orderService.createNormalOrder(dto))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void createNormalOrderSucceedsWithOnlyCustomerAddress() {
        CreateNormalOrderDto dto = CreateNormalOrderDto.builder()
                .customerName("Nguyen Van A")
                .customerMobile("0900000001")
                .customerAddress("123 Duong Le Loi")
                .customerCityName("Thành phố Hồ Chí Minh")
                .customerCityId(79L)
                .customerWardId(27154L)
                .carrierId(10L)
                .carrierServiceId(20L)
                .shippingFee(BigDecimal.ZERO)
                .items(List.of(CreateNormalOrderItemDto.builder()
                        .nhanhProductId("12345")
                        .name("Product A")
                        .price(new BigDecimal("100000"))
                        .quantity(1)
                        .build()))
                .build();
        when(orderRepository.findByOrderCode(anyString())).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryService.reserveForOrder(any(Order.class))).thenReturn(List.of());

        Order result = orderService.createNormalOrder(dto);

        assertThat(result.getCustomerAddress()).isEqualTo("123 Duong Le Loi");
        assertThat(result.getCustomerStreet()).isNull();
        assertThat(result.getCustomerHamlet()).isNull();
        assertThat(result.getCustomerCityId()).isEqualTo(79L);
        assertThat(result.getCustomerWardId()).isEqualTo(27154L);
    }

    @Test
    void createNormalOrderFallsBackToStreetAndHamletWhenAddressIsBlank() {
        CreateNormalOrderDto dto = CreateNormalOrderDto.builder()
                .customerName("Nguyen Van A")
                .customerMobile("0900000001")
                .customerStreet("Duong Le Loi")
                .customerHamlet("Thon 2")
                .customerCityName("Thành phố Hồ Chí Minh")
                .customerCityId(79L)
                .customerWardId(27154L)
                .carrierId(10L)
                .carrierServiceId(20L)
                .shippingFee(BigDecimal.ZERO)
                .items(List.of(CreateNormalOrderItemDto.builder()
                        .nhanhProductId("12345")
                        .name("Product A")
                        .price(new BigDecimal("100000"))
                        .quantity(1)
                        .build()))
                .build();
        when(orderRepository.findByOrderCode(anyString())).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryService.reserveForOrder(any(Order.class))).thenReturn(List.of());

        Order result = orderService.createNormalOrder(dto);

        assertThat(result.getCustomerAddress()).isEqualTo("Duong Le Loi, Thon 2");
    }

    @Test
    void createNormalOrderRejectsWhenAddressFieldsAreEmpty() {
        CreateNormalOrderDto dto = CreateNormalOrderDto.builder()
                .customerName("Nguyen Van A")
                .customerMobile("0900000001")
                .customerAddress("   ")
                .customerStreet("   ")
                .customerHamlet(null)
                .customerCityName("Thành phố Hồ Chí Minh")
                .customerCityId(79L)
                .customerWardId(27154L)
                .carrierId(10L)
                .carrierServiceId(20L)
                .shippingFee(BigDecimal.ZERO)
                .items(List.of(CreateNormalOrderItemDto.builder()
                        .nhanhProductId("12345")
                        .name("Product A")
                        .price(new BigDecimal("100000"))
                        .quantity(1)
                        .build()))
                .build();

        assertThatThrownBy(() -> orderService.createNormalOrder(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Customer address is required");
    }

    @Test
    void cancelMyOrderReleasesReservedStock() {
        Authentication auth = customerAuth();
        Account account = new Account();
        account.setEmail("customer@example.com");
        Order order = Order.builder()
                .id(202L)
                .customerEmail("customer@example.com")
                .status(OrderStatus.PROCESSING)
                .items(List.of(OrderItem.builder().productId(500L).quantity(2).build()))
                .build();

        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        when(orderRepository.findByIdForUpdate(202L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order result = orderService.cancelMyOrder(202L, auth);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventoryService).releaseForOrder(order);
    }

    @Test
    void updateOrderStatusAsAdminProcessingToShippedSucceeds() {
        Order order = Order.builder()
                .id(301L)
                .type(OrderType.NORMAL)
                .status(OrderStatus.PROCESSING)
                .build();

        when(orderRepository.findByIdForUpdate(301L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Authentication staffAuth = new UsernamePasswordAuthenticationToken("admin_user", "n/a");
        com.vn.sodu.order.dtos.UpdateOrderStatusDto dto = com.vn.sodu.order.dtos.UpdateOrderStatusDto.builder()
                .status(OrderStatus.SHIPPED)
                .trackingCode("SPXVN01234567")
                .reason("Shipped via SPX")
                .build();

        Order updated = orderService.updateOrderStatusAsAdmin(301L, dto, staffAuth);

        assertThat(updated.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(updated.getTrackingCode()).isEqualTo("SPXVN01234567");
        verify(auditService).record(
                eq(AuditAction.ORDER_STATUS_OVERRIDE),
                eq("ORDER"),
                eq("301"),
                eq("PROCESSING"),
                eq("SHIPPED"),
                contains("Shipped via SPX by admin_user")
        );
    }

    @Test
    void updateOrderStatusAsAdminProcessingToCancelledReleasesStock() {
        Order order = Order.builder()
                .id(302L)
                .type(OrderType.NORMAL)
                .status(OrderStatus.PROCESSING)
                .build();

        when(orderRepository.findByIdForUpdate(302L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Authentication staffAuth = new UsernamePasswordAuthenticationToken("staff_user", "n/a");
        com.vn.sodu.order.dtos.UpdateOrderStatusDto dto = com.vn.sodu.order.dtos.UpdateOrderStatusDto.builder()
                .status(OrderStatus.CANCELLED)
                .reason("Out of packaging materials")
                .build();

        Order cancelled = orderService.updateOrderStatusAsAdmin(302L, dto, staffAuth);

        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventoryService).releaseForOrder(cancelled);
        verify(eventPublisher).publishEvent(any(OrderCancelledEvent.class));
    }

    @Test
    void updateOrderStatusAsAdminInvalidTransitionThrowsException() {
        Order order = Order.builder()
                .id(303L)
                .type(OrderType.NORMAL)
                .status(OrderStatus.NEW)
                .build();

        when(orderRepository.findByIdForUpdate(303L)).thenReturn(Optional.of(order));

        com.vn.sodu.order.dtos.UpdateOrderStatusDto dto = com.vn.sodu.order.dtos.UpdateOrderStatusDto.builder()
                .status(OrderStatus.DELIVERED)
                .build();

        assertThatThrownBy(() -> orderService.updateOrderStatusAsAdmin(303L, dto, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid order transition from NEW to DELIVERED for NORMAL order");
    }

    private Authentication customerAuth() {
        return new UsernamePasswordAuthenticationToken("customer@example.com", "n/a");
    }
}
