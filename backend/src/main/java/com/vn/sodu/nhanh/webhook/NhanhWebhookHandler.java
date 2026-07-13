package com.vn.sodu.nhanh.webhook;

import com.fasterxml.jackson.databind.JsonNode;

public interface NhanhWebhookHandler {

    boolean supports(NhanhWebhookEvent event);

    void handle(NhanhWebhookEventLog eventLog, JsonNode data);
}
