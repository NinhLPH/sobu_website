package com.vn.sodu.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepo extends JpaRepository<Customer,Long> {
    boolean existsByAccountId(Long accountId);

    Optional<Customer> findByAccountId(Long accountId);
}
