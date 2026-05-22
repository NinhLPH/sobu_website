package com.vn.sodu.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
            where o.id = :orderId
              and o.customerMobile = :customerMobile
            """)
    Optional<Order> findCustomerOrderById(
            @Param("orderId") Long orderId,
            @Param("customerMobile") String customerMobile
    );

    @Query("""
            select distinct o from Order o
            left join fetch o.items
            left join fetch o.request
            where o.customerMobile = :customerMobile
              and (o.nhanhOrderId = :nhanhOrderId or o.nhanhOrderCode = :nhanhOrderId)
            """)
    Optional<Order> findCustomerOrderByNhanhOrderIdOrCode(
            @Param("nhanhOrderId") String nhanhOrderId,
            @Param("customerMobile") String customerMobile
    );
}
