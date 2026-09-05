package com.vn.sodu.order.dtos;

import com.vn.sodu.order.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload for manually updating order status")
public class UpdateOrderStatusDto {

    @NotNull(message = "Target status is required")
    @Schema(description = "Target order status according to the stage matrix", requiredMode = Schema.RequiredMode.REQUIRED)
    private OrderStatus status;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    @Schema(description = "Optional reason or note for updating status", example = "Customer requested priority shipment")
    private String reason;

    @Size(max = 255, message = "Tracking code must not exceed 255 characters")
    @Schema(description = "Tracking code / shipment code if status moves to SHIPPED", example = "SPXVN0491823719")
    private String trackingCode;
}
