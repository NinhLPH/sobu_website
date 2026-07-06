package com.vn.sodu.user;

import java.util.Optional;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepo extends JpaRepository<Account, Long> {
    @EntityGraph(attributePaths = "role")
    Optional<Account> findByEmail(String username);

    @Override
    @EntityGraph(attributePaths = "role")
    Optional<Account> findById(Long id);

    Optional<Account> findByPhone(String phone);
    Page<Account> findAll(Pageable pageable);
}
