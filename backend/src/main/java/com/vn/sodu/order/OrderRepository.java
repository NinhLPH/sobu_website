package com.vn.sodu.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderCode(String orderCode);
    Optional<Order> findByRequestId(Long requestId);

    @EntityGraph(attributePaths = {"items", "request"})
    Optional<Order> findWithItemsAndRequestById(Long id);
}
