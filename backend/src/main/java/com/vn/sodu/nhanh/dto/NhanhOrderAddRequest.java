package com.vn.sodu.nhanh.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NhanhOrderAddRequest {
    private Info info;
    private Channel channel;
    private ShippingAddress shippingAddress;
    private List<Product> products;
    private Carrier carrier;
    private Payment payment;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Info {
        private Long depotId;
        private Integer type;
        private String description;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Channel {
        private String appOrderId;
        private String sourceName;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingAddress {
        private String name;
        private String mobile;
        private String email;
        private String address;
        private Long cityId;
        private Long districtId;
        private Long wardId;
        private String locationVersion;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Product {
        private Long id;
        private String code;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal discount;
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
        private BigDecimal depositAmount;
        private Long depositAccountId;
        private BigDecimal transferAmount;
        private Long transferAccountId;
    }
}
