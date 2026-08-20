package com.vn.sodu.inventory;

import com.vn.sodu.audit.AuditService;
import com.vn.sodu.global.exception.BadRequestException;
import com.vn.sodu.global.exception.NotFoundException;
import com.vn.sodu.inventory.dto.InventoryAdjustmentDto;
import com.vn.sodu.inventory.dto.InventoryBalanceDto;
import com.vn.sodu.order.Order;
import com.vn.sodu.order.OrderItem;
import com.vn.sodu.product.Product;
import com.vn.sodu.product.repo.ProductRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepo productRepo;

    @Mock
    private InventoryLedgerRepository ledgerRepository;

    @Mock
    private AuditService auditService;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(productRepo, ledgerRepository, auditService);
        lenient().when(ledgerRepository.save(any(InventoryAdjustment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Product product(Long id, Double remain, Double available) {
        return Product.builder()
                .id(id)
                .stockRemain(remain)
                .stockAvailable(available)
                .build();
    }

    private void stubProduct(Product product) {
        when(productRepo.findByIdForUpdate(product.getId())).thenReturn(Optional.of(product));
    }

    @Test
    void setOpeningStockSetsBothBalancesAndRecordsLedger() {
        Product product = product(1L, null, null);
        stubProduct(product);

        InventoryAdjustmentDto dto = inventoryService.setOpeningStock(1L, 10.0, "Initial stock");

        assertThat(product.getStockRemain()).isEqualTo(10.0);
        assertThat(product.getStockAvailable()).isEqualTo(10.0);
        assertThat(dto.getType()).isEqualTo(InventoryAdjustmentType.OPENING_STOCK);
        assertThat(dto.getQuantityDelta()).isEqualTo(10.0);
        assertThat(dto.getBalanceAfter()).isEqualTo(10.0);
        verify(ledgerRepository).save(any(InventoryAdjustment.class));
        verify(auditService).record(eq(com.vn.sodu.audit.AuditAction.INVENTORY_ADJUSTMENT), eq("PRODUCT"), eq("1"),
                any(), any(), any());
    }

    @Test
    void stockInIncreasesBothBalances() {
        Product product = product(2L, 10.0, 10.0);
        stubProduct(product);

        InventoryAdjustmentDto dto = inventoryService.adjust(2L, InventoryAdjustmentType.STOCK_IN, 5.0, "Received goods");

        assertThat(product.getStockRemain()).isEqualTo(15.0);
        assertThat(product.getStockAvailable()).isEqualTo(15.0);
        assertThat(dto.getQuantityDelta()).isEqualTo(5.0);
        assertThat(dto.getBalanceAfter()).isEqualTo(15.0);
    }

    @Test
    void stockOutDecreasesBothBalances() {
        Product product = product(3L, 10.0, 7.0);
        stubProduct(product);

        InventoryAdjustmentDto dto = inventoryService.adjust(3L, InventoryAdjustmentType.STOCK_OUT, 4.0, "Shipped out");

        assertThat(product.getStockRemain()).isEqualTo(6.0);
        assertThat(product.getStockAvailable()).isEqualTo(3.0);
        assertThat(dto.getQuantityDelta()).isEqualTo(-4.0);
        assertThat(dto.getBalanceAfter()).isEqualTo(6.0);
    }

    @Test
    void correctionSetsPhysicalCountToAbsoluteTarget() {
        Product product = product(4L, 10.0, 10.0);
        stubProduct(product);

        InventoryAdjustmentDto dto = inventoryService.adjust(4L, InventoryAdjustmentType.CORRECTION, 8.0, "Counted");

        assertThat(product.getStockRemain()).isEqualTo(8.0);
        assertThat(product.getStockAvailable()).isEqualTo(8.0);
        assertThat(dto.getQuantityDelta()).isEqualTo(-2.0);
    }

    @Test
    void damagedDecreasesAndReturnedIncreasesStock() {
        Product damaged = product(5L, 10.0, 10.0);
        stubProduct(damaged);
        inventoryService.adjust(5L, InventoryAdjustmentType.DAMAGED, 1.0, "Broken");
        assertThat(damaged.getStockRemain()).isEqualTo(9.0);

        Product returned = product(6L, 9.0, 9.0);
        stubProduct(returned);
        inventoryService.adjust(6L, InventoryAdjustmentType.RETURNED, 2.0, "Customer return");
        assertThat(returned.getStockRemain()).isEqualTo(11.0);
    }

    @Test
    void stockOutBeyondZeroIsRejected() {
        Product product = product(7L, 5.0, 5.0);
        stubProduct(product);

        assertThatThrownBy(() -> inventoryService.adjust(7L, InventoryAdjustmentType.STOCK_OUT, 6.0, null))
                .isInstanceOf(BadRequestException.class);
        assertThat(product.getStockRemain()).isEqualTo(5.0);
        verify(ledgerRepository, never()).save(any(InventoryAdjustment.class));
    }

    @Test
    void negativeOrZeroAdjustmentQuantityIsRejected() {
        Product product = product(8L, 10.0, 10.0);
        stubProduct(product);

        assertThatThrownBy(() -> inventoryService.adjust(8L, InventoryAdjustmentType.STOCK_IN, -1.0, null))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> inventoryService.adjust(8L, InventoryAdjustmentType.STOCK_IN, 0.0, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void openingAndOrderTypesAreRejectedFromAdjustEndpoint() {
        Product product = product(9L, 10.0, 10.0);

        assertThatThrownBy(() -> inventoryService.adjust(9L, InventoryAdjustmentType.OPENING_STOCK, 10.0, null))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> inventoryService.adjust(9L, InventoryAdjustmentType.ORDER_RESERVATION, 1.0, null))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> inventoryService.adjust(9L, InventoryAdjustmentType.ORDER_RELEASE, 1.0, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void reserveForOrderDecrementsSellableStockAndRecordsOrderLedger() {
        Product product = product(10L, 10.0, 8.0);
        stubProduct(product);
        Order order = Order.builder()
                .id(77L)
                .orderCode("SOBU-ORD-77")
                .items(List.of(OrderItem.builder().productId(10L).quantity(3).build()))
                .build();
        when(ledgerRepository.existsByOrderIdAndTypeAndProductId(77L, InventoryAdjustmentType.ORDER_RESERVATION, 10L))
                .thenReturn(false);

        List<InventoryAdjustmentDto> entries = inventoryService.reserveForOrder(order);

        assertThat(product.getStockAvailable()).isEqualTo(5.0);
        assertThat(entries).hasSize(1);
        InventoryAdjustmentDto entry = entries.get(0);
        assertThat(entry.getType()).isEqualTo(InventoryAdjustmentType.ORDER_RESERVATION);
        assertThat(entry.getQuantityDelta()).isEqualTo(-3.0);
        assertThat(entry.getBalanceAfter()).isEqualTo(5.0);
        assertThat(entry.getOrderId()).isEqualTo(77L);
        assertThat(entry.getOrderCode()).isEqualTo("SOBU-ORD-77");
    }

    @Test
    void reserveForOrderIsIdempotentPerOrderAndProduct() {
        Product product = product(11L, 10.0, 8.0);
        Order order = Order.builder()
                .id(78L)
                .orderCode("SOBU-ORD-78")
                .items(List.of(OrderItem.builder().productId(11L).quantity(2).build()))
                .build();
        when(ledgerRepository.existsByOrderIdAndTypeAndProductId(78L, InventoryAdjustmentType.ORDER_RESERVATION, 11L))
                .thenReturn(true);

        List<InventoryAdjustmentDto> entries = inventoryService.reserveForOrder(order);

        assertThat(entries).isEmpty();
        assertThat(product.getStockAvailable()).isEqualTo(8.0);
        verify(ledgerRepository, never()).save(any(InventoryAdjustment.class));
    }

    @Test
    void reserveForOrderRejectsWhenStockIsInsufficient() {
        Product product = product(12L, 10.0, 2.0);
        stubProduct(product);
        Order order = Order.builder()
                .id(79L)
                .orderCode("SOBU-ORD-79")
                .items(List.of(OrderItem.builder().productId(12L).quantity(5).build()))
                .build();
        when(ledgerRepository.existsByOrderIdAndTypeAndProductId(79L, InventoryAdjustmentType.ORDER_RESERVATION, 12L))
                .thenReturn(false);

        assertThatThrownBy(() -> inventoryService.reserveForOrder(order))
                .isInstanceOf(InsufficientStockException.class);
        assertThat(product.getStockAvailable()).isEqualTo(2.0);
    }

    @Test
    void releaseForOrderRestoresSellableStockCappedAtPhysical() {
        Product product = product(13L, 10.0, 7.0);
        stubProduct(product);
        Order order = Order.builder()
                .id(80L)
                .orderCode("SOBU-ORD-80")
                .items(List.of(OrderItem.builder().productId(13L).quantity(3).build()))
                .build();
        when(ledgerRepository.existsByOrderIdAndTypeAndProductId(80L, InventoryAdjustmentType.ORDER_RELEASE, 13L))
                .thenReturn(false);

        List<InventoryAdjustmentDto> entries = inventoryService.releaseForOrder(order);

        assertThat(product.getStockAvailable()).isEqualTo(10.0);
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getType()).isEqualTo(InventoryAdjustmentType.ORDER_RELEASE);
        assertThat(entries.get(0).getQuantityDelta()).isEqualTo(3.0);
    }

    @Test
    void releaseForOrderCannotExceedPhysicalStock() {
        Product product = product(14L, 10.0, 10.0);
        stubProduct(product);
        Order order = Order.builder()
                .id(81L)
                .orderCode("SOBU-ORD-81")
                .items(List.of(OrderItem.builder().productId(14L).quantity(5).build()))
                .build();
        when(ledgerRepository.existsByOrderIdAndTypeAndProductId(81L, InventoryAdjustmentType.ORDER_RELEASE, 14L))
                .thenReturn(false);

        inventoryService.releaseForOrder(order);

        assertThat(product.getStockAvailable()).isEqualTo(10.0);
    }

    @Test
    void getBalanceComputesReservedFromRemainAndAvailable() {
        Product product = product(15L, 10.0, 7.0);
        when(productRepo.findByIdForUpdate(15L)).thenReturn(Optional.of(product));

        InventoryBalanceDto balance = inventoryService.getBalance(15L);

        assertThat(balance.getStockRemain()).isEqualTo(10.0);
        assertThat(balance.getStockAvailable()).isEqualTo(7.0);
        assertThat(balance.getReserved()).isEqualTo(3.0);
    }

    @Test
    void missingProductThrowsNotFound() {
        when(productRepo.findByIdForUpdate(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> inventoryService.getBalance(99L))
                .isInstanceOf(NotFoundException.class);
    }
}