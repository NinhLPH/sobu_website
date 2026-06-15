package com.vn.sodu.customer.royalty;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "loyalty_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long customerId;

    @Enumerated(EnumType.STRING)
    private Type type; // EARN, REDEEM, ADJUST

    private Integer points; // có thể âm

    @Enumerated(EnumType.STRING)
    private Source source; // ORDER, ADMIN, PROMOTION

    private Long referenceId; // orderId

    private String note;

    private LocalDateTime createdAt;

    public enum Type {
        EARN, REDEEM, ADJUST
    }

    public enum Source {
        ORDER, ADMIN, PROMOTION
    }
}
