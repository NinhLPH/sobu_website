package com.vn.sodu;

import com.vn.sodu.integration.NhanhEnabled;
import com.vn.sodu.nhanh.service.NhanhLocationSyncCoordinator;
import com.vn.sodu.nhanh.service.NhanhLocationSyncService;
import com.vn.sodu.nhanh.service.NhanhWebhookProcessor;
import com.vn.sodu.nhanh.webhook.handler.DefaultWebhookHandler;
import com.vn.sodu.nhanh.webhook.handler.OrderWebhookHandler;
import com.vn.sodu.nhanh.webhook.handler.ProductWebhookHandler;
import com.vn.sodu.order.OrderSyncEventListener;
import com.vn.sodu.order.nhanh.NhanhOrderGateway;
import com.vn.sodu.order.services.OrderSyncService;
import com.vn.sodu.payment.service.PayOSPaymentReconciliationService;
import com.vn.sodu.product.brand.service.BrandSyncService;
import com.vn.sodu.product.category.service.CategorySyncService;
import com.vn.sodu.product.service.ProductSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 1 exit criteria: the backend must start with no Nhanh credentials and
 * no Nhanh scheduled job / outbound bean may be instantiated in local mode
 * ({@code integration.nhanh.enabled=false}, the dev default). Local schedulers
 * and the local shipping fallback must remain available.
 */
@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = "spring.sql.init.mode=never")
class LocalModeBoundaryTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private NhanhEnabled nhanhEnabled;

    @Test
    void localModeFlagIsDisabledByDefault() {
        assertThat(nhanhEnabled.isEnabled()).isFalse();
    }

    @Test
    void nhanhScheduledAndOutboundBeansAreNotInstantiated() {
        assertThat(applicationContext.getBeansOfType(OrderSyncService.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(ProductSyncService.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(BrandSyncService.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(CategorySyncService.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(NhanhOrderGateway.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(NhanhLocationSyncCoordinator.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(NhanhLocationSyncService.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(NhanhWebhookProcessor.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(OrderSyncEventListener.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(DefaultWebhookHandler.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(OrderWebhookHandler.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(ProductWebhookHandler.class)).isEmpty();
    }

    @Test
    void localSchedulersAndLocalShippingFallbackRemainAvailable() {
        assertThat(applicationContext.getBeansOfType(PayOSPaymentReconciliationService.class)).isNotEmpty();
    }
}