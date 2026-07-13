package com.vn.sodu.order.services;

import com.vn.sodu.order.Order;
import com.vn.sodu.order.OrderStatus;
import com.vn.sodu.order.OrderCustomerEmailMatcher;
import com.vn.sodu.order.repo.OrderRepository;
import com.vn.sodu.order.mapper.OrderResponseMapper;
import com.vn.sodu.order.dtos.OrderResponseDto;
import com.vn.sodu.order.dtos.CustomerOrderListItemDto;
import com.vn.sodu.user.Account;
import com.vn.sodu.user.AccountRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private static final Set<String> CUSTOMER_SORT_FIELDS = Set.of("createdAt", "totalAmount", "status");

    private final OrderRepository orderRepository;
    private final OrderResponseMapper orderResponseMapper;
    private final AccountRepo accountRepo;

    @Transactional(readOnly = true)
    public Page<OrderResponseDto> listOrders(int page, int size, String sortBy, String sortDirection) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDirection);
        return orderRepository.findAll(pageable).map(orderResponseMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<CustomerOrderListItemDto> listMyOrders(
            Authentication authentication,
            int page,
            int size,
            String query,
            String status,
            LocalDate createdFrom,
            LocalDate createdTo,
            String sortBy,
            String sortDirection
    ) {
        String customerEmail = resolveCustomerEmail(authentication);
        String safeQuery = query == null || query.isBlank() ? null : query.trim();
        OrderStatus parsedStatus = parseOrderStatus(status);
        LocalDateTime from = createdFrom == null ? null : createdFrom.atStartOfDay();
        LocalDateTime toExclusive = createdTo == null ? null : createdTo.plusDays(1).atStartOfDay();
        Pageable pageable = buildCustomerPageable(page, size, sortBy, sortDirection);

        return orderRepository.findCustomerOrders(
                customerEmail,
                safeQuery,
                parsedStatus,
                from,
                toExclusive,
                pageable
        ).map(CustomerOrderListItemDto::from);
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getOrderDetail(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order id is required");
        }

        Order order = orderRepository.findWithItemsAndRequestById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        return orderResponseMapper.toDto(order);
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getMyOrderDetail(Long orderId, Authentication authentication) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order id is required");
        }

        String customerEmail = resolveCustomerEmail(authentication);
        Order order = orderRepository.findWithItemsAndRequestById(orderId)
                .filter(existingOrder -> OrderCustomerEmailMatcher.matches(existingOrder.getCustomerEmail(), customerEmail))
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        return orderResponseMapper.toDto(order);
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getMyOrderDetailByNhanhOrderId(String nhanhOrderId, Authentication authentication) {
        if (nhanhOrderId == null || nhanhOrderId.isBlank()) {
            throw new IllegalArgumentException("Nhanh order id is required");
        }

        String customerEmail = resolveCustomerEmail(authentication);
        String safeNhanhOrderId = nhanhOrderId.trim();
        Order order = orderRepository.findWithItemsAndRequestByNhanhOrderIdOrCode(safeNhanhOrderId)
                .filter(existingOrder -> OrderCustomerEmailMatcher.matches(existingOrder.getCustomerEmail(), customerEmail))
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + safeNhanhOrderId));
        return orderResponseMapper.toDto(order);
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDirection) {
        String safeSortBy = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(sortDirection == null ? "DESC" : sortDirection);
        } catch (IllegalArgumentException ex) {
            direction = Sort.Direction.DESC;
        }
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? Math.min(size, 100) : 20;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, safeSortBy));
    }

    private Pageable buildCustomerPageable(int page, int size, String sortBy, String sortDirection) {
        String safeSortBy = CUSTOMER_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(sortDirection == null ? "DESC" : sortDirection);
        } catch (IllegalArgumentException ex) {
            direction = Sort.Direction.DESC;
        }
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? Math.min(size, 100) : 10;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, safeSortBy));
    }

    private OrderStatus parseOrderStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return null;
        }
        try {
            return OrderStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported order status: " + status);
        }
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
