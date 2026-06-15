package com.vn.sodu.nhanh.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NhanhOrderEditRequest {
    private Info info;
    private Carrier carrier;
    private Payment payment;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Info {
        private Long id;
        private String description;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Carrier {
        private Long id;
        private Long serviceId;
        private BigDecimal customerShipFee;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payment {
        private BigDecimal transferAmount;
        private Long transferAccountId;
        private String code;
    }
}
