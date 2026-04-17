package com.vn.sodu.customer.royalty.service.impl;

import com.vn.sodu.customer.royalty.LoyaltyTier;
import com.vn.sodu.customer.royalty.repo.LoyaltyRuleRepo;
import com.vn.sodu.customer.royalty.repo.LoyaltyTierRepo;
import com.vn.sodu.customer.royalty.service.interf.LoyaltyRuleEngine;
import org.springframework.beans.factory.annotation.Autowired;

public class LoyaltyRuleEngineImpl implements LoyaltyRuleEngine {
    @Autowired
    private LoyaltyRuleRepo ruleRepo;

    @Autowired
    private LoyaltyTierRepo tierRepo;

    @Override
    public int calculateEarnPoints(double orderAmount) {
        // ví dụ value = "10000:1"
        String rule = ruleRepo.findByCode("EARN_RATE")
                .orElseThrow()
                .getValue();

        String[] parts = rule.split(":");
        double moneyPerPoint = Double.parseDouble(parts[0]);
        int pointPerUnit = Integer.parseInt(parts[1]);

        return (int) (orderAmount / moneyPerPoint) * pointPerUnit;
    }

    @Override
    public LoyaltyTier resolveTier(double totalMoney) {
        return tierRepo.findTopByMinTotalMoneyLessThanEqualOrderByMinTotalMoneyDesc(totalMoney);
    }
}
