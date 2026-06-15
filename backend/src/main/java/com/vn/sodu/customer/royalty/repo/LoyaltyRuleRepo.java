package com.vn.sodu.customer.royalty.repo;

import com.vn.sodu.customer.royalty.LoyaltyRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoyaltyRuleRepo extends JpaRepository<LoyaltyRule, Long> {
    Optional<LoyaltyRule> findByCode(String earnRate);
}
