package com.vn.sodu.order;

import com.vn.sodu.order.services.OrderSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSyncEventListener {

    private final OrderSyncService orderSyncService;

    @Async("nhanhSyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderReadyForSync(OrderReadyForSyncEvent event) {
        if (event == null || event.orderId() == null) {
            log.warn("Skipping Nhanh order sync event with missing order id");
            return;
        }
        orderSyncService.syncOrderToNhanh(event.orderId());
    }
}
