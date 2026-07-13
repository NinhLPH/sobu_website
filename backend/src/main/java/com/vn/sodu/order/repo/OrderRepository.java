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
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
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

    @Query("""
            select o from Order o
            where lower(trim(o.customerEmail)) = lower(:customerEmail)
              and (:query is null
                   or lower(o.orderCode) like lower(concat('%', :query, '%'))
                   or lower(coalesce(o.nhanhOrderId, '')) like lower(concat('%', :query, '%'))
                   or lower(coalesce(o.nhanhOrderCode, '')) like lower(concat('%', :query, '%')))
              and (:status is null or o.status = :status)
              and (:createdFrom is null or o.createdAt >= :createdFrom)
              and (:createdToExclusive is null or o.createdAt < :createdToExclusive)
            """)
    Page<Order> findCustomerOrders(
            @Param("customerEmail") String customerEmail,
            @Param("query") String query,
            @Param("status") com.vn.sodu.order.OrderStatus status,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdToExclusive") LocalDateTime createdToExclusive,
            Pageable pageable
    );
}
