package com.vn.sodu.nhanh.webhook.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEvent;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEventLog;
import com.vn.sodu.nhanh.webhook.NhanhWebhookHandler;
import com.vn.sodu.product.Product;
import com.vn.sodu.product.dto.NhanhProductDTO;
import com.vn.sodu.product.repo.ProductRepo;
import com.vn.sodu.product.service.ProductSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductWebhookHandler implements NhanhWebhookHandler {

    private final ProductSyncService productSyncService;
    private final ProductRepo productRepo;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(NhanhWebhookEvent event) {
        return switch (event) {
            case PRODUCT_ADD, PRODUCT_UPDATE, PRODUCT_DELETE, INVENTORY_CHANGE -> true;
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
            case PRODUCT_ADD, PRODUCT_UPDATE -> handleProductAddOrUpdate(data);
            case PRODUCT_DELETE -> handleProductDelete(data);
            case INVENTORY_CHANGE -> handleInventoryChange(data);
        }
    }

    private void handleProductAddOrUpdate(JsonNode data) {
        if (data == null || data.isNull()) {
            return;
        }
        try {
            NhanhProductDTO dto = objectMapper.treeToValue(data, NhanhProductDTO.class);
            productSyncService.syncOne(dto);
            log.info("Product webhook processed: externalId={}", dto.getId());
        } catch (Exception ex) {
            log.warn("Failed to process product add/update webhook", ex);
            throw new RuntimeException("Failed to process product add/update webhook", ex);
        }
    }

    private void handleProductDelete(JsonNode data) {
        if (data == null || !data.isArray()) {
            return;
        }
        for (JsonNode idNode : data) {
            Long productId = idNode.asLong();
            productRepo.findByExternalId(productId).ifPresent(product -> {
                product.setActive(false);
                productRepo.save(product);
                log.info("Product soft-deleted by webhook: externalId={}", productId);
            });
        }
    }

    private void handleInventoryChange(JsonNode data) {
        if (data == null || !data.isArray()) {
            return;
        }
        for (JsonNode item : data) {
            Long productId = item.get("id").asLong();
            double remain = item.get("remain").asDouble();
            double available = item.get("available").asDouble();

            productRepo.findByExternalId(productId).ifPresent(product -> {
                product.setStockRemain(remain);
                product.setStockAvailable(available);
                productRepo.save(product);
                log.info("Inventory updated by webhook: externalId={}, remain={}, available={}",
                        productId, remain, available);
            });
        }
    }
}
