package com.vn.sodu.integration;

import com.vn.sodu.audit.AuditAction;
import com.vn.sodu.audit.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Records the deployment-only integration flag state each time the application
 * starts. The flag ({@code integration.nhanh.enabled}) is only changeable via
 * the deployment/operations configuration, so every boot effectively captures
 * the current deployment decision in the audit trail.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IntegrationFlagChangeAuditor {

    private final NhanhEnabled nhanhEnabled;
    private final AuditService auditService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        auditService.record(
                AuditAction.INTEGRATION_FLAG_CHANGE,
                "INTEGRATION_FLAG",
                "integration.nhanh.enabled",
                "not recorded at runtime (deployment configuration)",
                "enabled=" + nhanhEnabled.isEnabled(),
                "Application startup; the integration flag is deployment-only configuration"
        );
    }
}
