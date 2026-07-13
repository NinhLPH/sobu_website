package com.vn.sodu.nhanh.webhook.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEvent;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEventLog;
import com.vn.sodu.nhanh.webhook.NhanhWebhookHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

import static com.vn.sodu.nhanh.webhook.NhanhWebhookEvent.INVENTORY_CHANGE;
import static com.vn.sodu.nhanh.webhook.NhanhWebhookEvent.ORDER_ADD;
import static com.vn.sodu.nhanh.webhook.NhanhWebhookEvent.ORDER_DELETE;
import static com.vn.sodu.nhanh.webhook.NhanhWebhookEvent.ORDER_PARTIAL_RETURN;
import static com.vn.sodu.nhanh.webhook.NhanhWebhookEvent.ORDER_UPDATE;
import static com.vn.sodu.nhanh.webhook.NhanhWebhookEvent.PRODUCT_ADD;
import static com.vn.sodu.nhanh.webhook.NhanhWebhookEvent.PRODUCT_DELETE;
import static com.vn.sodu.nhanh.webhook.NhanhWebhookEvent.PRODUCT_UPDATE;

@Slf4j
@Component
public class DefaultWebhookHandler implements NhanhWebhookHandler {

    private static final Set<NhanhWebhookEvent> HANDLED_BY_SPECIFIC = Set.of(
            PRODUCT_ADD, PRODUCT_UPDATE, PRODUCT_DELETE, INVENTORY_CHANGE,
            ORDER_ADD, ORDER_UPDATE, ORDER_DELETE, ORDER_PARTIAL_RETURN
    );

    @Override
    public boolean supports(NhanhWebhookEvent event) {
        return !HANDLED_BY_SPECIFIC.contains(event);
    }

    @Override
    public void handle(NhanhWebhookEventLog eventLog, JsonNode data) {
        log.info("Processed Nhanh webhook event={}, businessId={}, externalObjectId={}",
                eventLog.getEventName(),
                eventLog.getBusinessId(),
                eventLog.getExternalObjectId());
    }
}
