package com.vn.sodu.nhanh.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Raw shipping fee option returned from Nhanh POS API")
public class NhanhShippingFeeOption {
    @JsonAlias({"carrierId", "id"})
    @Schema(description = "Carrier ID", example = "22151")
    private Long carrierId;

    @JsonAlias({"carrierName", "name"})
    @Schema(description = "Carrier name", example = "Viettel Post")
    private String carrierName;

    @JsonAlias({"carrierServiceId", "serviceId"})
    @Schema(description = "Carrier service ID", example = "1")
    private Long carrierServiceId;

    @JsonAlias({"carrierServiceName", "serviceName"})
    @Schema(description = "Carrier service name", example = "VCN")
    private String carrierServiceName;

    @Schema(description = "Shipping fee", example = "35000.00")
    private BigDecimal shipFee;

    @Schema(description = "Customer shipping fee", example = "35000.00")
    private BigDecimal customerShipFee;

    @JsonAlias({"deliveryTime", "estimatedDeliveryTime"})
    @Schema(description = "Estimated delivery time", example = "2-3 ngày")
    private String deliveryTime;

    @Schema(description = "Additional description", example = "Giao hàng tiết kiệm")
    private String description;

    @JsonProperty("carrier")
    public void setCarrier(JsonNode carrier) {
        if (carrier == null || carrier.isNull()) {
            return;
        }
        JsonNode id = carrier.get("id");
        if (id != null && id.canConvertToLong()) {
            this.carrierId = id.asLong();
        }
        JsonNode name = carrier.get("name");
        if (name != null && !name.isNull()) {
            this.carrierName = name.asText();
        }
    }

    @JsonProperty("service")
    public void setService(JsonNode service) {
        if (service == null || service.isNull()) {
            return;
        }
        JsonNode id = service.get("id");
        if (id != null && id.canConvertToLong()) {
            this.carrierServiceId = id.asLong();
        }
        JsonNode name = service.get("name");
        if (name != null && !name.isNull()) {
            this.carrierServiceName = name.asText();
        }
        JsonNode description = service.get("description");
        if (description != null && !description.isNull() && this.description == null) {
            this.description = description.asText();
        }
    }
}
