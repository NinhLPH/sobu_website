package com.vn.sodu.customer.royalty.service.interf;

import com.vn.sodu.customer.royalty.LoyaltyTier;

public interface LoyaltyRuleEngine {
    int calculateEarnPoints(double orderAmount);

    LoyaltyTier resolveTier(double totalMoney);
}
