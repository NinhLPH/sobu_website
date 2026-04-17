package com.vn.sodu.customer.royalty;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "loyalty_tiers")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double minTotalMoney; // điều kiện lên hạng

    private Double discountRate; // optional (%)

    public enum TierName{
        SILVER, GOLD, PLATIUM
    }
}
