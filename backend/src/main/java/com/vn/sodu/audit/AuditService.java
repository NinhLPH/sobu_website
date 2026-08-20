package com.vn.sodu.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Records sensitive local operations to the audit trail. Actor is resolved from
 * the current security context, falling back to "system" for background and
 * deployment flows. Audit persistence failures are logged but never break the
 * audited business operation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final OperationAuditRepository repository;

    public void record(AuditAction action, String targetType, String targetId,
                       String beforeValue, String afterValue, String reason) {
        try {
            repository.save(toEntity(action, targetType, targetId, beforeValue, afterValue, reason));
        } catch (RuntimeException ex) {
            log.error("Failed to record audit for action={} target={}/{}: {}",
                    action, targetType, targetId, ex.getMessage());
        }
    }

    private OperationAudit toEntity(AuditAction action, String targetType, String targetId,
                                    String beforeValue, String afterValue, String reason) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = authentication != null && authentication.isAuthenticated();
        String actor = authentication != null && authentication.getName() != null
                ? authentication.getName()
                : "system";

        return OperationAudit.builder()
                .action(action)
                .actor(actor)
                .actorType(authenticated ? "account" : "system")
                .targetType(targetType)
                .targetId(targetId)
                .beforeValue(beforeValue)
                .afterValue(afterValue)
                .reason(reason)
                .build();
    }
}
