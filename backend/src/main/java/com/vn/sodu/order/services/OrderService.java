package com.vn.sodu.order.services;

import com.vn.sodu.order.*;
import com.vn.sodu.order.dtos.CreateNormalOrderDto;
import com.vn.sodu.order.dtos.CreateNormalOrderItemDto;
import com.vn.sodu.order.mapper.RequestToOrderMapper;
import com.vn.sodu.order.repo.OrderRepository;
import com.vn.sodu.payment.PaymentType;
import com.vn.sodu.payment.PaymentMethod;
import com.vn.sodu.payment.service.PaymentService;
import com.vn.sodu.request.OrderType;
import com.vn.sodu.request.Request;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
            .orElseThrow(() -> new IllegalArgumentException("Customer resolution failed for phone: " + request.getCustomerPhone()));
        // use mail to resolve customer
        // 3. Validate for conversion
        orderConversionPolicy.validateForConversion(request, customer);

        // 4. Map to internal Order
        Order newOrder = requestToOrderMapper.mapToOrder(request, customer);
        paymentService.initializeOrderPaymentState(newOrder);
        applyInitialPreorderStatus(newOrder);

        // 5. Save order
        Order savedOrder = orderRepository.save(newOrder);
        createInitialPreorderDepositIfRequired(savedOrder);
        return savedOrder;
    }

    @Transactional
    public Order createNormalOrder(CreateNormalOrderDto dto) {
        validateDirectOrder(dto);
        String orderCode = generateUniqueOrderCode();

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
                .customerAddress(trim(dto.getCustomerAddress()))
                .customerCityName(trim(dto.getCustomerCityName()))
                .customerDistrictName(trim(dto.getCustomerDistrictName()))
                .customerWardName(trim(dto.getCustomerWardName()))
                .customerCityId(dto.getCustomerCityId())
                .customerDistrictId(dto.getCustomerDistrictId())
                .customerWardId(dto.getCustomerWardId())
                .carrierId(dto.getCarrierId())
                .carrierServiceId(dto.getCarrierServiceId())
                .shippingFee(money(dto.getShippingFee()))
                .description(trim(dto.getDescription()))
                .depositAmount(money(BigDecimal.ZERO))
                .items(new ArrayList<>())
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (CreateNormalOrderItemDto itemDto : dto.getItems()) {
            BigDecimal price = money(itemDto.getPrice());
            BigDecimal discount = money(itemDto.getDiscount());
            int quantity = itemDto.getQuantity();
            BigDecimal lineTotal = price.subtract(discount).max(BigDecimal.ZERO).multiply(BigDecimal.valueOf(quantity));
            total = total.add(lineTotal);

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .nhanhProductId(trim(itemDto.getNhanhProductId()))
                    .name(trim(itemDto.getName()))
                    .note(trim(itemDto.getNote()))
                    .price(price)
                    .discount(discount)
                    .quantity(quantity)
                    .build();
            order.getItems().add(item);
        }

        order.setTotalAmount(money(total.add(money(dto.getShippingFee()))));
        paymentService.initializeOrderPaymentState(order);
        return orderRepository.save(order);
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
            String nhanhProductId = trim(item.getNhanhProductId());
            if (nhanhProductId == null || nhanhProductId.isBlank()) {
                throw new IllegalArgumentException("Nhanh product id is required");
            }
            if (!nhanhProductId.chars().allMatch(Character::isDigit)) {
                throw new IllegalArgumentException("Nhanh product id must be numeric");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Item quantity must be at least 1");
            }
            if (item.getPrice() == null || item.getPrice().signum() < 0) {
                throw new IllegalArgumentException("Item price must be greater than or equal to 0");
            }
            if (item.getDiscount() != null && item.getDiscount().signum() < 0) {
                throw new IllegalArgumentException("Item discount must be greater than or equal to 0");
            }
        }
    }

    private String generateUniqueOrderCode() {
        for (int i = 0; i < 20; i++) {
            String code = "SOBU-ORD-" + LocalDateTime.now().format(ORDER_CODE_FORMATTER) + "-" + String.format("%04d", RANDOM.nextInt(10_000));
            if (orderRepository.findByOrderCode(code).isEmpty()) {
                return code;
            }
        }
        throw new IllegalStateException("Unable to generate unique order code");
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private void applyInitialPreorderStatus(Order order) {
        if (requiresPreorderDeposit(order)) {
            order.setStatus(OrderStatus.WAITING_DEPOSIT);
        }
    }

    private void createInitialPreorderDepositIfRequired(Order order) {
        if (requiresPreorderDeposit(order)) {
            paymentService.createPayment(order, PaymentType.DEPOSIT, PaymentMethod.ONLINE);
        }
    }

    private boolean requiresPreorderDeposit(Order order) {
        return order != null
                && order.getType() == OrderType.PREORDER
                && money(order.getDepositAmount()).compareTo(BigDecimal.ZERO) > 0;
    }
}
