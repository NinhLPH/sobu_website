package com.vn.sodu.nhanh.webhook.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEvent;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEventLog;
import com.vn.sodu.nhanh.webhook.NhanhWebhookHandler;
import com.vn.sodu.nhanh.webhook.NhanhOrderStatusMapper;
import com.vn.sodu.order.Order;
import com.vn.sodu.order.OrderStatus;
import com.vn.sodu.order.repo.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderWebhookHandler implements NhanhWebhookHandler {

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(NhanhWebhookEvent event) {
        return switch (event) {
            case ORDER_ADD, ORDER_UPDATE, ORDER_DELETE -> true;
            default -> false;
        };
    }

    @Override
    public void handle(NhanhWebhookEventLog eventLog, JsonNode data) {
        NhanhWebhookEvent event = NhanhWebhookEvent.from(eventLog.getEventName()).orElse(null);
        if (event == null) {
            return;
        }

        switch (event) {
            case ORDER_ADD -> handleOrderAdd(data);
            case ORDER_UPDATE -> handleOrderUpdate(data);
            case ORDER_DELETE -> handleOrderDelete(data);
        }
    }

    void handleOrderAdd(JsonNode data) {
        if (data == null || data.isNull()) {
            return;
        }

        JsonNode channel = data.get("channel");
        String appOrderId = channel != null ? channel.get("appOrderId").asText(null) : null;
        if (appOrderId == null || appOrderId.isBlank()) {
            log.warn("orderAdd webhook missing appOrderId");
            return;
        }

        Optional<Order> existing = orderRepository.findByAppOrderId(appOrderId);
        if (existing.isEmpty()) {
            log.warn("orderAdd webhook: no local order found for appOrderId={}", appOrderId);
            return;
        }

        Order order = existing.get();
        if (order.getNhanhOrderId() != null && "SYNCED".equals(order.getSyncStatus().name())) {
            log.info("orderAdd webhook: order {} already synced, skipping", appOrderId);
            return;
        }

        JsonNode info = data.get("info");
        if (info != null) {
            JsonNode idNode = info.get("id");
            if (idNode != null && !idNode.isNull()) {
                order.setNhanhOrderId(idNode.asText());
            }
        }

        order.setSyncStatus(com.vn.sodu.order.OrderSyncStatus.SYNCED);
        order.setLastSyncAt(LocalDateTime.now());
        order.setLastSyncMessage("Synced via orderAdd webhook");
        orderRepository.save(order);

        log.info("Order synced via orderAdd webhook: appOrderId={}, nhanhOrderId={}",
                appOrderId, order.getNhanhOrderId());
    }

    void handleOrderUpdate(JsonNode data) {
        if (data == null || data.isNull()) {
            return;
        }

        JsonNode info = data.get("info");
        if (info == null) {
            log.warn("orderUpdate webhook missing info");
            return;
        }

        JsonNode idNode = info.get("id");
        if (idNode == null || idNode.isNull()) {
            log.warn("orderUpdate webhook missing info.id");
            return;
        }
        String nhanhOrderId = idNode.asText();

        Optional<Order> existing = orderRepository.findWithItemsAndRequestByNhanhOrderIdOrCode(nhanhOrderId);
        if (existing.isEmpty()) {
            log.warn("orderUpdate webhook: no local order found for nhanhOrderId={}", nhanhOrderId);
            return;
        }

        Order order = existing.get();

        // Map status
        JsonNode statusNode = info.get("status");
        if (statusNode != null && !statusNode.isNull()) {
            int nhanhCode = statusNode.asInt();
            OrderStatus mappedStatus = NhanhOrderStatusMapper.mapToLocal(nhanhCode);
            if (mappedStatus != null) {
                order.setStatus(mappedStatus);
                log.info("Order status updated via webhook: nhanhOrderId={}, nhanhCode={}, newStatus={}",
                        nhanhOrderId, nhanhCode, mappedStatus);
            } else {
                log.warn("Unknown Nhanh order status code: {} for nhanhOrderId={}", nhanhCode, nhanhOrderId);
            }
        }

        // Update carrier info
        JsonNode carrier = data.get("carrier");
        if (carrier != null && carrier.isObject()) {
            JsonNode carrierIdNode = carrier.get("id");
            if (carrierIdNode != null && !carrierIdNode.isNull()) {
                order.setCarrierId(carrierIdNode.asLong());
            }

            JsonNode trackingUrlNode = carrier.get("trackingUrl");
            if (trackingUrlNode != null && !trackingUrlNode.isNull()) {
                order.setTrackingUrl(trackingUrlNode.asText());
            } else {
                JsonNode carrierCodeNode = carrier.get("carrierCode");
                if (carrierCodeNode != null && !carrierCodeNode.isNull()) {
                    order.setTrackingUrl(carrierCodeNode.asText());
                }
            }
        }

        order.setLastSyncAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    void handleOrderDelete(JsonNode data) {
        if (data == null || !data.isArray()) {
            return;
        }

        for (JsonNode idNode : data) {
            String nhanhOrderId = idNode.asText();
            Optional<Order> existing = orderRepository.findWithItemsAndRequestByNhanhOrderIdOrCode(nhanhOrderId);

            if (existing.isEmpty()) {
                log.warn("orderDelete webhook: no local order found for nhanhOrderId={}", nhanhOrderId);
                continue;
            }

            Order order = existing.get();
            order.setStatus(OrderStatus.CANCELLED);
            order.setLastSyncAt(LocalDateTime.now());
            order.setLastSyncMessage("Cancelled via orderDelete webhook");
            orderRepository.save(order);

            log.info("Order cancelled via orderDelete webhook: nhanhOrderId={}", nhanhOrderId);
        }
    }
}
