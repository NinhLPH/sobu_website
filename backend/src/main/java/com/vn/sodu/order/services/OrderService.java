package com.vn.sodu.order.services;

import com.vn.sodu.order.*;
import com.vn.sodu.audit.AuditAction;
import com.vn.sodu.audit.AuditService;
import com.vn.sodu.inventory.InventoryService;
import com.vn.sodu.integration.NhanhEnabled;
import com.vn.sodu.location.AddressContract;
import com.vn.sodu.global.exception.ForbiddenOperationException;
import com.vn.sodu.order.dtos.CreateNormalOrderDto;
import com.vn.sodu.order.dtos.CreateNormalOrderItemDto;
import com.vn.sodu.order.dtos.UpdateOrderStatusDto;
import com.vn.sodu.order.policy.OrderTransitionPolicy;
import com.vn.sodu.order.mapper.RequestToOrderMapper;
import com.vn.sodu.order.repo.OrderRepository;
import com.vn.sodu.payment.PaymentType;
import com.vn.sodu.payment.PaymentMethod;
import com.vn.sodu.payment.service.PaymentCheckoutCreationException;
import com.vn.sodu.payment.service.PaymentService;
import com.vn.sodu.product.Product;
import com.vn.sodu.product.service.ProductPricing;
import com.vn.sodu.product.repo.ProductRepo;
import com.vn.sodu.request.OrderType;
import com.vn.sodu.request.Request;
import com.vn.sodu.user.Account;
import com.vn.sodu.user.AccountRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter ORDER_CODE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderRepository orderRepository;
    private final OrderConversionPolicy orderConversionPolicy;
    private final OrderCustomerResolver orderCustomerResolver;
    private final RequestToOrderMapper requestToOrderMapper;
    private final PaymentService paymentService;
    private final AccountRepo accountRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final NhanhEnabled nhanhEnabled;
    private final AuditService auditService;
    private final com.vn.sodu.location.AddressService addressService;
    private final ProductRepo productRepo;
    private final InventoryService inventoryService;
    private final com.vn.sodu.voucher.service.VoucherService voucherService;
    private final OrderTransitionPolicy orderTransitionPolicy;

    @Transactional
    public Order createFromApprovedRequest(Request request) {
        // 1. Check idempotency
        Optional<Order> existingOrder = orderConversionPolicy.getExistingOrder(request);
        if (existingOrder.isPresent()) {
            log.info("Order already exists for request {}, returning existing order.", request.getId());
            return existingOrder.get();
        }

        // 2. Resolve customer
        ResolvedOrderCustomer customer = orderCustomerResolver.resolveByPhone(request.getCustomerPhone())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Customer resolution failed for phone: " + request.getCustomerPhone()));
        // use mail to resolve customer
        // 3. Validate for conversion
        orderConversionPolicy.validateForConversion(request, customer);

        // 4. Map to internal Order
        Order newOrder = requestToOrderMapper.mapToOrder(request, customer);
        paymentService.initializeOrderPaymentState(newOrder);
        applyInitialPreorderStatus(newOrder);

        // 5. Save order
        Order savedOrder = orderRepository.save(newOrder);
        // 6. Reserve local sellable stock for order items that resolve to a local
        // product.
        // A failed reservation rolls back the whole order.
        inventoryService.reserveForOrder(savedOrder);
        createInitialPreorderDepositIfRequired(savedOrder);
        return savedOrder;
    }

    @Transactional
    public Order createNormalOrder(CreateNormalOrderDto dto) {
        validateDirectOrder(dto);
        String orderCode = generateUniqueOrderCode();
        String resolvedAddress = resolveCustomerAddress(dto);

        Order order = Order.builder()
                .orderCode(orderCode)
                .appOrderId(orderCode)
                .request(null)
                .type(OrderType.NORMAL)
                .status(OrderStatus.NEW)
                .syncStatus(OrderSyncStatus.PENDING)
                .nhanhSyncStage(NhanhSyncStage.NONE)
                .customerName(trim(dto.getCustomerName()))
                .customerMobile(trim(dto.getCustomerMobile()))
                .customerEmail(trim(dto.getCustomerEmail()))
                .customerAddress(resolvedAddress)
                .customerStreet(trim(dto.getCustomerStreet()))
                .customerHamlet(trim(dto.getCustomerHamlet()))
                .customerCityName(trim(dto.getCustomerCityName()))
                .customerDistrictName(trim(dto.getCustomerDistrictName()))
                .customerWardName(trim(dto.getCustomerWardName()))
                .customerCityId(dto.getCustomerCityId())
                .customerDistrictId(dto.getCustomerDistrictId())
                .customerWardId(dto.getCustomerWardId())
                .locationVersion(AddressContract.resolveVersionForWrite(dto.getLocationVersion()))
                .carrierId(dto.getCarrierId())
                .carrierServiceId(dto.getCarrierServiceId())
                .shippingFee(money(dto.getShippingFee()))
                .description(trim(dto.getDescription()))
                .depositAmount(money(BigDecimal.ZERO))
                .items(new ArrayList<>())
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (CreateNormalOrderItemDto itemDto : dto.getItems()) {
            ResolvedOrderItem resolved = resolveOrderItem(itemDto);
            BigDecimal price = resolved.price();
            BigDecimal discount = money(BigDecimal.ZERO);
            int quantity = itemDto.getQuantity();
            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(quantity));
            total = total.add(lineTotal);

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .productId(resolved.productId())
                    .nhanhProductId(resolved.nhanhProductId())
                    .name(resolved.name())
                    .note(trim(itemDto.getNote()))
                    .price(price)
                    .discount(discount)
                    .quantity(quantity)
                    .build();
            order.getItems().add(item);
        }

        BigDecimal subtotal = money(total);
        BigDecimal shippingFee = money(dto.getShippingFee());

        List<com.vn.sodu.voucher.dto.VoucherCartItemDto> voucherItems = order.getItems().stream()
                .map(it -> com.vn.sodu.voucher.dto.VoucherCartItemDto.builder()
                        .productId(it.getProductId())
                        .categoryId(it.getProductId() != null
                                ? productRepo.findById(it.getProductId()).map(Product::getCategoryId).orElse(null)
                                : null)
                        .name(it.getName())
                        .price(it.getPrice())
                        .quantity(it.getQuantity())
                        .build())
                .toList();

        com.vn.sodu.voucher.dto.VoucherApplyRequestDto voucherReq = com.vn.sodu.voucher.dto.VoucherApplyRequestDto
                .builder()
                .discountVoucherCode(dto.getDiscountVoucherCode())
                .shippingVoucherCode(dto.getShippingVoucherCode())
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .items(voucherItems)
                .customerCityName(order.getCustomerCityName())
                .customerDistrictName(order.getCustomerDistrictName())
                .customerWardName(order.getCustomerWardName())
                .customerCityId(order.getCustomerCityId())
                .customerDistrictId(order.getCustomerDistrictId())
                .customerWardId(order.getCustomerWardId())
                .autoApply(true)
                .build();

        com.vn.sodu.voucher.dto.VoucherApplyResponseDto voucherResp = voucherService.applyVouchers(voucherReq);
        if (!voucherResp.isValid()) {
            throw new IllegalArgumentException(voucherResp.getMessage());
        }

        order.setDiscountVoucherCode(voucherResp.getDiscountVoucherCode());
        order.setShippingVoucherCode(voucherResp.getShippingVoucherCode());
        order.setDiscountAmount(money(voucherResp.getSubtotalDiscount()));
        order.setShippingDiscountAmount(money(voucherResp.getShippingDiscount()));
        order.setTotalAmount(money(voucherResp.getFinalTotal()));

        paymentService.initializeOrderPaymentState(order);
        Order savedOrder = orderRepository.save(order);

        // Record voucher usage counts atomically
        voucherService.recordVoucherUsage(
                voucherResp.getItemVoucherCode(),
                voucherResp.getOrderVoucherCode(),
                voucherResp.getShippingVoucherCode());

        // Reserve sellable stock atomically. A failed reservation rolls back the order.
        inventoryService.reserveForOrder(savedOrder);
        return savedOrder;
    }

    @Transactional
    public Order cancelMyOrder(Long orderId, Authentication authentication) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order id is required");
        }

        String customerEmail = resolveCustomerEmail(authentication);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .filter(existingOrder -> OrderCustomerEmailMatcher.matches(existingOrder.getCustomerEmail(),
                        customerEmail))
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (!isCustomerCancelable(order.getStatus())) {
            throw new ForbiddenOperationException("Không thể hủy đơn khi đơn hàng đã chuyển sang trạng thái giao hàng");
        }

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        Order cancelled = orderRepository.save(order);
        inventoryService.releaseForOrder(cancelled);
        auditService.record(
                AuditAction.ORDER_STATUS_OVERRIDE,
                "ORDER",
                String.valueOf(cancelled.getId()),
                previousStatus.name(),
                cancelled.getStatus().name(),
                "Customer cancellation");
        if (nhanhEnabled.isEnabled()) {
            eventPublisher.publishEvent(new OrderCancelledEvent(cancelled.getId()));
        }
        return cancelled;
    }

    /**
     * Updates only the fulfilment milestones that are owned by staff in local mode.
     * Payment-related statuses remain exclusively controlled by the payment flow.
     */
    @Transactional
    public Order updateFulfilmentStatusByStaff(Long orderId, OrderStatus targetStatus) {
        if (orderId == null || targetStatus == null) {
            throw new IllegalArgumentException("Order id and target status are required");
        }
        if (nhanhEnabled.isEnabled()) {
            throw new ForbiddenOperationException(
                    "Trạng thái đơn đang được đồng bộ từ Nhanh.vn và không thể cập nhật thủ công.");
        }

        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        OrderStatus previousStatus = order.getStatus();
        if (!isAllowedStaffFulfilmentTransition(previousStatus, targetStatus)) {
            throw new IllegalStateException("Chỉ có thể chuyển đơn theo luồng Mới → Đang xử lý → Đang giao → Đã giao.");
        }

        order.setStatus(targetStatus);
        Order updatedOrder = orderRepository.save(order);
        auditService.record(
                AuditAction.ORDER_STATUS_OVERRIDE,
                "ORDER",
                String.valueOf(updatedOrder.getId()),
                previousStatus.name(),
                updatedOrder.getStatus().name(),
                "Staff fulfilment status update");
        return updatedOrder;
    }

    @Transactional
    public Order updateOrderStatusAsAdmin(Long orderId, UpdateOrderStatusDto dto, Authentication authentication) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order id is required");
        }
        if (dto == null || dto.getStatus() == null) {
            throw new IllegalArgumentException("Target status is required");
        }

        Order order = orderRepository.findByIdForUpdateWithItems(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        OrderStatus currentStatus = order.getStatus();
        OrderStatus targetStatus = dto.getStatus();

        orderTransitionPolicy.validateTransition(order.getType(), currentStatus, targetStatus);

        if (currentStatus == targetStatus) {
            if (!isEmpty(dto.getTrackingCode())) {
                order.setTrackingCode(dto.getTrackingCode().trim());
                if (isEmpty(order.getTrackingUrl())) {
                    order.setTrackingUrl(dto.getTrackingCode().trim());
                }
                return orderRepository.save(order);
            }
            return order;
        }

        String operator = (authentication != null && authentication.getName() != null
                && !authentication.getName().isBlank())
                        ? authentication.getName().trim()
                        : "staff";

        if (targetStatus == OrderStatus.CANCELLED) {
            order.setStatus(OrderStatus.CANCELLED);
            Order cancelled = orderRepository.save(order);
            inventoryService.releaseForOrder(cancelled);
            String note = !isEmpty(dto.getReason()) ? dto.getReason().trim() : "Admin manual status update";
            auditService.record(
                    AuditAction.ORDER_STATUS_OVERRIDE,
                    "ORDER",
                    String.valueOf(cancelled.getId()),
                    currentStatus.name(),
                    targetStatus.name(),
                    note + " by " + operator);
            if (nhanhEnabled.isEnabled()) {
                eventPublisher.publishEvent(new OrderCancelledEvent(cancelled.getId()));
            }
            return cancelled;
        }

        if (!isEmpty(dto.getTrackingCode())) {
            order.setTrackingCode(dto.getTrackingCode().trim());
            if (isEmpty(order.getTrackingUrl())) {
                order.setTrackingUrl(dto.getTrackingCode().trim());
            }
        }

        order.setStatus(targetStatus);
        Order updated = orderRepository.save(order);

        String note = !isEmpty(dto.getReason()) ? dto.getReason().trim() : "Admin manual status update";
        auditService.record(
                AuditAction.ORDER_STATUS_OVERRIDE,
                "ORDER",
                String.valueOf(updated.getId()),
                currentStatus.name(),
                targetStatus.name(),
                note + " by " + operator);

        return updated;
    }

    private void validateDirectOrder(CreateNormalOrderDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Create order payload is required");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least one order item is required");
        }
        for (CreateNormalOrderItemDto item : dto.getItems()) {
            if (item == null) {
                throw new IllegalArgumentException("Order item is required");
            }
            Long productId = item.getProductId();
            String nhanhProductId = trim(item.getNhanhProductId());
            if (productId == null && (nhanhProductId == null || nhanhProductId.isBlank())) {
                throw new IllegalArgumentException("Either a product id or a Nhanh product id is required");
            }
            if (productId != null && productId <= 0) {
                throw new IllegalArgumentException("Product id must be greater than 0");
            }
            if (nhanhProductId != null && !nhanhProductId.isBlank()
                    && !nhanhProductId.chars().allMatch(Character::isDigit)) {
                throw new IllegalArgumentException("Nhanh product id must be numeric");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Item quantity must be at least 1");
            }
            if (item.getPrice() != null && item.getPrice().signum() < 0) {
                throw new IllegalArgumentException("Item price must be greater than or equal to 0");
            }
            if (item.getDiscount() != null && item.getDiscount().signum() < 0) {
                throw new IllegalArgumentException("Item discount must be greater than or equal to 0");
            }
        }
        if (dto.getCustomerCityId() == null || dto.getCustomerCityId() <= 0
                || dto.getCustomerWardId() == null || dto.getCustomerWardId() <= 0) {
            throw new IllegalArgumentException("Customer city and ward ids are required");
        }
        String resolvedAddress = resolveCustomerAddress(dto);
        if (isEmpty(resolvedAddress)) {
            throw new IllegalArgumentException("Customer address is required");
        }
        if (!addressService.isWardInProvince(dto.getCustomerWardId(), dto.getCustomerCityId())) {
            throw new IllegalArgumentException("Ward does not belong to the selected province");
        }
        if (dto.getCarrierId() == null || dto.getCarrierId() <= 0
                || dto.getCarrierServiceId() == null || dto.getCarrierServiceId() <= 0) {
            throw new IllegalArgumentException("Carrier id and carrier service id are required");
        }
        if (dto.getShippingFee() == null || dto.getShippingFee().signum() < 0) {
            throw new IllegalArgumentException("Shipping fee must be greater than or equal to 0");
        }
    }

    private boolean isAllowedStaffFulfilmentTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
        return (currentStatus == OrderStatus.NEW && targetStatus == OrderStatus.PROCESSING)
                || (currentStatus == OrderStatus.PROCESSING && targetStatus == OrderStatus.SHIPPED)
                || (currentStatus == OrderStatus.SHIPPED && targetStatus == OrderStatus.DELIVERED);
    }

    private ResolvedOrderItem resolveOrderItem(CreateNormalOrderItemDto itemDto) {
        String nhanhProductId = trim(itemDto.getNhanhProductId());
        if (itemDto.getProductId() != null) {
            Product product = productRepo.findById(itemDto.getProductId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Product not found with id: " + itemDto.getProductId()));
            return new ResolvedOrderItem(product.getId(), null, product.getName(),
                    money(ProductPricing.effectivePrice(product)));
        }
        if (nhanhProductId != null && !nhanhProductId.isBlank()) {
            try {
                Optional<Product> product = productRepo.findByExternalId(Long.parseLong(nhanhProductId));
                if (product.isPresent()) {
                    Product found = product.get();
                    return new ResolvedOrderItem(found.getId(), nhanhProductId, found.getName(),
                            money(ProductPricing.effectivePrice(found)));
                }
            } catch (NumberFormatException ignored) {
                // fall through to legacy snapshot
            }
        }
        // Legacy path: item does not resolve to a local product; keep the provided
        // snapshot.
        return new ResolvedOrderItem(null, nhanhProductId, trim(itemDto.getName()), money(itemDto.getPrice()));
    }

    private record ResolvedOrderItem(Long productId, String nhanhProductId, String name, BigDecimal price) {
    }

    private String generateUniqueOrderCode() {
        for (int i = 0; i < 20; i++) {
            String code = "SOBU-ORD-" + LocalDateTime.now().format(ORDER_CODE_FORMATTER) + "-"
                    + String.format("%04d", RANDOM.nextInt(10_000));
            if (orderRepository.findByOrderCode(code).isEmpty()) {
                return code;
            }
        }
        throw new IllegalStateException("Unable to generate unique order code");
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String resolveCustomerAddress(CreateNormalOrderDto dto) {
        if (dto == null) {
            return null;
        }
        String address = trim(dto.getCustomerAddress());
        if (!isEmpty(address)) {
            return address;
        }
        String street = trim(dto.getCustomerStreet());
        String hamlet = trim(dto.getCustomerHamlet());
        if (!isEmpty(street) && !isEmpty(hamlet)) {
            return street + ", " + hamlet;
        }
        if (!isEmpty(street)) {
            return street;
        }
        if (!isEmpty(hamlet)) {
            return hamlet;
        }
        return null;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void applyInitialPreorderStatus(Order order) {
        if (requiresPreorderDeposit(order)) {
            order.setStatus(OrderStatus.WAITING_DEPOSIT);
        }
    }

    private void createInitialPreorderDepositIfRequired(Order order) {
        if (requiresPreorderDeposit(order)) {
            try {
                paymentService.createPayment(order, PaymentType.DEPOSIT, PaymentMethod.ONLINE);
            } catch (PaymentCheckoutCreationException ex) {
                log.warn(
                        "Failed to create initial preorder deposit checkout for order id={}: {}",
                        order.getId(),
                        ex.getMessage());
            }
        }
    }

    private boolean requiresPreorderDeposit(Order order) {
        return order != null
                && order.getType() == OrderType.PREORDER
                && money(order.getDepositAmount()).compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean isCustomerCancelable(OrderStatus status) {
        return status != OrderStatus.SHIPPED
                && status != OrderStatus.DELIVERED
                && status != OrderStatus.CANCELLED;
    }

    private String resolveCustomerEmail(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new AccessDeniedException("Authentication is required");
        }

        Account account = accountRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated account not found"));

        if (account.getEmail() == null || account.getEmail().isBlank()) {
            throw new AccessDeniedException("Authenticated account does not have an email address");
        }
        return account.getEmail().trim();
    }
}
