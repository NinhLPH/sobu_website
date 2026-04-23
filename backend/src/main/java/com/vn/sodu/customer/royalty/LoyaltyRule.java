package com.vn.sodu.customer.royalty;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "loyalty_rules")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code; // EARN_RATE

    private String value; // ví dụ: "10000:1"

    private Boolean active;
}
