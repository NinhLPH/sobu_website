package com.vn.sodu.inventory;

import com.vn.sodu.order.Order;
import com.vn.sodu.order.OrderItem;
import com.vn.sodu.order.repo.OrderRepository;
import com.vn.sodu.product.Product;
import com.vn.sodu.product.repo.ProductRepo;
import com.vn.sodu.request.OrderType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 4 exit criterion: two concurrent checkouts must not sell more units
 * than are available. Runs against a real database (MySQL in CI) so the
 * pessimistic row locks actually serialize concurrent reservations.
 */
@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = "spring.sql.init.mode=never")
class InventoryConcurrencyTest {

    private static final int AVAILABLE = 5;
    private static final int WORKERS = 12;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryLedgerRepository ledgerRepository;

    @Test
    void concurrentReservationsCannotOversell() throws Exception {
        Product product = productRepo.save(Product.builder()
                .name("Concurrency Product")
                .code("CONC-PROD-001")
                .status("ACTIVE")
                .active(true)
                .stockRemain((double) AVAILABLE)
                .stockAvailable((double) AVAILABLE)
                .build());

        ExecutorService executor = Executors.newFixedThreadPool(WORKERS);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < WORKERS; i++) {
                final int worker = i;
                futures.add(executor.submit(() -> {
                    try {
                        start.await();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    Order order = Order.builder()
                            .orderCode("CONC-ORD-" + worker + "-" + System.nanoTime())
                            .appOrderId("CONC-ORD-" + worker + "-" + System.nanoTime())
                            .type(OrderType.NORMAL)
                            .build();
                    order.setItems(List.of(OrderItem.builder()
                            .order(order)
                            .productId(product.getId())
                            .name("Concurrency Product")
                            .quantity(1)
                            .build()));
                    orderRepository.save(order);
                    try {
                        inventoryService.reserveForOrder(order);
                        successes.incrementAndGet();
                    } catch (InsufficientStockException expected) {
                        // over-subscribed worker: reservation correctly rejected
                    }
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        Product refreshed = productRepo.findById(product.getId()).orElseThrow();
        assertThat(successes.get()).isEqualTo(AVAILABLE);
        assertThat(refreshed.getStockAvailable()).isEqualTo(0.0);
        assertThat(ledgerRepository.countByProductIdAndType(
                product.getId(), InventoryAdjustmentType.ORDER_RESERVATION)).isEqualTo(AVAILABLE);
    }
}