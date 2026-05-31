package com.vn.sodu.order.services;

import com.vn.sodu.order.Order;
import com.vn.sodu.order.repo.OrderRepository;
import com.vn.sodu.order.mapper.OrderResponseMapper;
import com.vn.sodu.order.dtos.OrderResponseDto;
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

@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final OrderResponseMapper orderResponseMapper;
    private final AccountRepo accountRepo;

    @Transactional(readOnly = true)
    public Page<OrderResponseDto> listOrders(int page, int size, String sortBy, String sortDirection) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDirection);
        return orderRepository.findAll(pageable).map(orderResponseMapper::toDto);
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

        String customerPhone = resolveCustomerPhone(authentication);
        Order order = orderRepository.findCustomerOrderById(orderId, customerPhone)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        return orderResponseMapper.toDto(order);
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getMyOrderDetailByNhanhOrderId(String nhanhOrderId, Authentication authentication) {
        if (nhanhOrderId == null || nhanhOrderId.isBlank()) {
            throw new IllegalArgumentException("Nhanh order id is required");
        }

        String customerPhone = resolveCustomerPhone(authentication);
        String safeNhanhOrderId = nhanhOrderId.trim();
        Order order = orderRepository.findCustomerOrderByNhanhOrderIdOrCode(safeNhanhOrderId, customerPhone)
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

    private String resolveCustomerPhone(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new AccessDeniedException("Authentication is required");
        }

        Account account = accountRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated account not found"));

        if (account.getPhone() == null || account.getPhone().isBlank()) {
            throw new AccessDeniedException("Authenticated account does not have a phone number");
        }
        return account.getPhone().trim();
    }
}
