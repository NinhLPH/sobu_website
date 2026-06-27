package com.vn.sodu.nhanh.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class NhanhShippingFeeOption {
    @JsonAlias({"carrierId", "id"})
    private Long carrierId;

    @JsonAlias({"carrierName", "name"})
    private String carrierName;

    @JsonAlias({"carrierServiceId", "serviceId"})
    private Long carrierServiceId;

    @JsonAlias({"carrierServiceName", "serviceName"})
    private String carrierServiceName;

    private BigDecimal shipFee;
    private BigDecimal customerShipFee;

    @JsonAlias({"deliveryTime", "estimatedDeliveryTime"})
    private String deliveryTime;

    private String description;
}
