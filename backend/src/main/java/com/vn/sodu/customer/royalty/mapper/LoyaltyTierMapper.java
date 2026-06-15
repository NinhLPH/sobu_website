package com.vn.sodu.customer.royalty.mapper;

import com.vn.sodu.customer.royalty.LoyaltyTier;
import com.vn.sodu.customer.royalty.dto.CreateLoyaltyTierRequest;
import com.vn.sodu.customer.royalty.dto.LoyaltyTierDTO;
import com.vn.sodu.customer.royalty.dto.UpdateLoyaltyTierRequest;
import org.springframework.stereotype.Component;

@Component
public class LoyaltyTierMapper {

    /**
     * Convert LoyaltyTier entity to LoyaltyTierDTO
     */
    public LoyaltyTierDTO toDTO(LoyaltyTier tier) {
        if (tier == null) {
            return null;
        }
        return LoyaltyTierDTO.builder()
                .id(tier.getId())
                .minTotalMoney(tier.getMinTotalMoney())
                .discountRate(tier.getDiscountRate())
                .tierName(tier.getId() != null ? getTierName(tier.getId()) : null)
                .build();
    }

    /**
     * Convert LoyaltyTierDTO to LoyaltyTier entity
     */
    public LoyaltyTier toEntity(LoyaltyTierDTO dto) {
        if (dto == null) {
            return null;
        }
        return LoyaltyTier.builder()
                .id(dto.getId())
                .minTotalMoney(dto.getMinTotalMoney())
                .discountRate(dto.getDiscountRate())
                .build();
    }

    /**
     * Convert CreateLoyaltyTierRequest to LoyaltyTier entity
     */
    public LoyaltyTier toEntity(CreateLoyaltyTierRequest request) {
        if (request == null) {
            return null;
        }
        return LoyaltyTier.builder()
                .minTotalMoney(request.getMinTotalMoney())
                .discountRate(request.getDiscountRate())
                .build();
    }

    /**
     * Convert UpdateLoyaltyTierRequest to LoyaltyTier entity
     */
    public LoyaltyTier toEntity(UpdateLoyaltyTierRequest request) {
        if (request == null) {
            return null;
        }
        return LoyaltyTier.builder()
                .minTotalMoney(request.getMinTotalMoney())
                .discountRate(request.getDiscountRate())
                .build();
    }

    /**
     * Get tier name from tier id
     */
    private String getTierName(Long tierId) {
        if (tierId == 1L) {
            return LoyaltyTier.TierName.SILVER.name();
        } else if (tierId == 2L) {
            return LoyaltyTier.TierName.GOLD.name();
        } else if (tierId == 3L) {
            return LoyaltyTier.TierName.PLATIUM.name();
        }
        return null;
    }
}
