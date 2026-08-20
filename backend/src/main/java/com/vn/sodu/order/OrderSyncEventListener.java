package com.vn.sodu.order;

import com.vn.sodu.integration.NhanhEnabled;
import com.vn.sodu.order.services.OrderSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@ConditionalOnProperty(name = "integration.nhanh.enabled", havingValue = "true")
@Component
@RequiredArgsConstructor
public class OrderSyncEventListener {

    private final OrderSyncService orderSyncService;
    private final NhanhEnabled nhanhEnabled;

    @Async("nhanhSyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderReadyForSync(OrderReadyForSyncEvent event) {
        if (!nhanhEnabled.isEnabled()) {
            return;
        }
        if (event == null || event.orderId() == null) {
            log.warn("Skipping Nhanh order sync event with missing order id");
            return;
        }
        orderSyncService.syncOrderToNhanh(event.orderId(), event.paymentCode());
    }

    @Async("nhanhSyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCancelled(OrderCancelledEvent event) {
        if (!nhanhEnabled.isEnabled()) {
            return;
        }
        if (event == null || event.orderId() == null) {
            log.warn("Skipping Nhanh order cancel event with missing order id");
            return;
        }
        orderSyncService.cancelOrderOnNhanh(event.orderId());
    }
}
