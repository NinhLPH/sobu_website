package com.vn.sodu.audit;

/**
 * Sensitive local operations that must be audited from the first Local Mode
 * release. Actions without a local write path yet (e.g. catalog mutation,
 * inventory adjustment, shipping-policy change) are reserved here so later
 * phases can reuse the same audit service.
 */
public enum AuditAction {
    /** Create/update/deactivate/archive of a product, category or brand. */
    CATALOG_MUTATION,
    /** Manual stock-in/out, correction or damaged/returned adjustment. */
    INVENTORY_ADJUSTMENT,
    /** Order status change or override (payment advance, cancellation, staff). */
    ORDER_STATUS_OVERRIDE,
    /** Shipping policy / flat-fee or carrier configuration change. */
    SHIPPING_POLICY_CHANGE,
    /** Deployment-only integration flag change (integration.nhanh.enabled). */
    INTEGRATION_FLAG_CHANGE
}
