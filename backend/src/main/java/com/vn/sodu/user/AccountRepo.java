package com.vn.sodu.user;

import java.util.Optional;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepo extends JpaRepository<Account, Long> {
    Optional<Account> findByEmail(String email);
    Optional<Account> findByUsernameAndStatus(String username, Account.AccountStatus status);
    Optional<Account> findByIdAndStatus(Long id, Account.AccountStatus status);
    Page<Account> findAll(Pageable pageable);
}
