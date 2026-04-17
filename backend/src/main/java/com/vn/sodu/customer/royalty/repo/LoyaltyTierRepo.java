package com.vn.sodu.customer.royalty.repo;

import com.vn.sodu.customer.royalty.LoyaltyTier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoyaltyTierRepo extends JpaRepository<LoyaltyTier,Long> {
    LoyaltyTier findTopByMinTotalMoneyLessThanEqualOrderByMinTotalMoneyDesc(double totalMoney);
}
