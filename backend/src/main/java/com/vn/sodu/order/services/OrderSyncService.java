package com.vn.sodu.order.services;

import com.vn.sodu.nhanh.service.NhanhService;
import com.vn.sodu.nhanh.dto.NhanhOrderAddResult;
import com.vn.sodu.order.Order;
import com.vn.sodu.order.repo.OrderRepository;
import com.vn.sodu.order.OrderSyncStatus;
import com.vn.sodu.order.nhanh.NhanhOrderGateway;
import com.vn.sodu.request.OrderType;
import com.vn.sodu.request.Request;
import com.vn.sodu.request.repo.RequestRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSyncService {

    private static final int MAX_SYNC_ERROR_LENGTH = 1000;

    private final OrderRepository orderRepository;
    private final RequestRepo requestRepo;
    private final NhanhService nhanhService;
    private final NhanhOrderGateway nhanhOrderGateway;
    private final TransactionTemplate transactionTemplate;

    public void syncOrderToNhanh(Long orderId) {
        Order order = orderRepository.findWithItemsAndRequestById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getType() != OrderType.NORMAL) {
            log.info("Skipping Nhanh sync for non-NORMAL order id={}, type={}", order.getId(), order.getType());
            return;
        }
        if (order.getSyncStatus() == OrderSyncStatus.SYNCED) {
            log.info("Skipping Nhanh sync for already synced order id={}", order.getId());
            return;
        }

        try {
            String accessToken = nhanhService.getValidAccessToken();
            NhanhOrderAddResult result = nhanhOrderGateway.createOrder(order, accessToken);
            transactionTemplate.executeWithoutResult(status -> markSynced(orderId, result));
        } catch (Exception ex) {
            transactionTemplate.executeWithoutResult(status -> markFailed(orderId, ex));
        }
    }

    public Order retryOrderSync(Long orderId) {
        syncOrderToNhanh(orderId);
        return orderRepository.findWithItemsAndRequestById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    private void markSynced(Long orderId, NhanhOrderAddResult result) {
        Order order = orderRepository.findWithItemsAndRequestById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        String nhanhOrderId = result == null || result.getId() == null ? null : result.getId().toString();
        String nhanhOrderCode = order.getOrderCode();

        order.setNhanhOrderId(nhanhOrderId);
        order.setNhanhOrderCode(nhanhOrderCode);
        order.setSyncStatus(OrderSyncStatus.SYNCED);
        order.setSyncError(result != null && result.isDuplicate()
                ? "Nhanh duplicate appOrderId; treated as synced."
                : null);
        orderRepository.save(order);

        Request request = order.getRequest();
        if (request != null && request.getId() != null) {
            request.setNhanhOrderId(nhanhOrderId);
            request.setNhanhOrderCode(nhanhOrderCode);
            requestRepo.save(request);
        }
    }

    private void markFailed(Long orderId, Exception ex) {
        Order order = orderRepository.findWithItemsAndRequestById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        order.setSyncStatus(OrderSyncStatus.FAILED);
        order.setSyncError(truncate(message));
        orderRepository.save(order);
        log.warn("Nhanh order sync failed for order id={}, code={}: {}", order.getId(), order.getOrderCode(), message);
    }

    private String truncate(String message) {
        if (message == null || message.length() <= MAX_SYNC_ERROR_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_SYNC_ERROR_LENGTH);
    }
}
