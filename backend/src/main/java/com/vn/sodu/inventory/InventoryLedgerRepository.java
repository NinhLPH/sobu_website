package com.vn.sodu.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryLedgerRepository extends JpaRepository<InventoryAdjustment, Long> {

    List<InventoryAdjustment> findByProductIdOrderByIdDesc(Long productId);

    List<InventoryAdjustment> findByOrderId(Long orderId);

    long countByProductIdAndType(Long productId, InventoryAdjustmentType type);

    boolean existsByOrderIdAndTypeAndProductId(Long orderId, InventoryAdjustmentType type, Long productId);
}