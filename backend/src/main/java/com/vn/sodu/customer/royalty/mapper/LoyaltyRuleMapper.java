package com.vn.sodu.customer.royalty.mapper;

import com.vn.sodu.customer.royalty.LoyaltyRule;
import com.vn.sodu.customer.royalty.dto.CreateLoyaltyRuleRequest;
import com.vn.sodu.customer.royalty.dto.LoyaltyRuleDTO;
import com.vn.sodu.customer.royalty.dto.UpdateLoyaltyRuleRequest;
import org.springframework.stereotype.Component;

@Component
public class LoyaltyRuleMapper {

    /**
     * Convert LoyaltyRule entity to LoyaltyRuleDTO
     */
    public LoyaltyRuleDTO toDTO(LoyaltyRule rule) {
        if (rule == null) {
            return null;
        }
        return LoyaltyRuleDTO.builder()
                .id(rule.getId())
                .code(rule.getCode())
                .value(rule.getValue())
                .active(rule.getActive())
                .build();
    }

    /**
     * Convert LoyaltyRuleDTO to LoyaltyRule entity
     */
    public LoyaltyRule toEntity(LoyaltyRuleDTO dto) {
        if (dto == null) {
            return null;
        }
        return LoyaltyRule.builder()
                .id(dto.getId())
                .code(dto.getCode())
                .value(dto.getValue())
                .active(dto.getActive())
                .build();
    }

    /**
     * Convert CreateLoyaltyRuleRequest to LoyaltyRule entity
     */
    public LoyaltyRule toEntity(CreateLoyaltyRuleRequest request) {
        if (request == null) {
            return null;
        }
        return LoyaltyRule.builder()
                .code(request.getCode())
                .value(request.getValue())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();
    }

    /**
     * Convert UpdateLoyaltyRuleRequest to LoyaltyRule entity
     */
    public LoyaltyRule toEntity(UpdateLoyaltyRuleRequest request) {
        if (request == null) {
            return null;
        }
        return LoyaltyRule.builder()
                .code(request.getCode())
                .value(request.getValue())
                .active(request.getActive())
                .build();
    }
}
