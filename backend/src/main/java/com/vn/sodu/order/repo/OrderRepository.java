package com.vn.sodu.order.repo;

import com.vn.sodu.order.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderCode(String orderCode);
    Optional<Order> findByAppOrderId(String appOrderId);
    Optional<Order> findByRequestId(Long requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

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

    @Query("""
            select distinct o from Order o
            left join fetch o.items
            left join fetch o.request
            where o.status = com.vn.sodu.order.OrderStatus.DELIVERED
              and lower(trim(o.customerEmail)) = lower(:customerEmail)
            order by o.updatedAt desc
            """)
    List<Order> findDeliveredCustomerOrdersForReview(@Param("customerEmail") String customerEmail);
}
