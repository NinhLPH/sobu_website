package com.vn.sodu.customer;


import com.vn.sodu.customer.royalty.LoyaltyTier;
import com.vn.sodu.user.Account;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer gender;
    private LocalDate birthday;

    // Address
    private String province;
    private String district;
    private String ward;
    private String street;

    // Loyalty summary
    private Double totalMoney = 0.0;
    private Integer points = 0;

    @ManyToOne
    @JoinColumn(name = "tier_id")
    private LoyaltyTier tier;

    @OneToOne
    @JoinColumn(name = "account_id", unique = true)
    private Account account;
}