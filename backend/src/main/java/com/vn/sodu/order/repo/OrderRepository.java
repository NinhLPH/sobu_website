package com.vn.sodu.order.repo;

import com.vn.sodu.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderCode(String orderCode);
    Optional<Order> findByRequestId(Long requestId);

    @EntityGraph(attributePaths = {"items", "request"})
    Optional<Order> findWithItemsAndRequestById(Long id);

    @Query("""
            select distinct o from Order o
            left join fetch o.items
            left join fetch o.request
            where o.nhanhOrderId = :nhanhOrderId or o.nhanhOrderCode = :nhanhOrderId
            """)
    Optional<Order> findWithItemsAndRequestByNhanhOrderIdOrCode(
            @Param("nhanhOrderId") String nhanhOrderId
    );

    java.util.List<Order> findBySyncStatusInOrderByUpdatedAtAsc(Collection<com.vn.sodu.order.OrderSyncStatus> statuses, Pageable pageable);
}
