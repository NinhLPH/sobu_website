package com.vn.sodu.request.dto;

import com.vn.sodu.request.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ProcessRequestDto {

    @NotNull(message = "Target status is required")
    @Schema(description = "Target request status after processing", example = "APPROVED")
    private RequestStatus targetStatus;

    @Size(max = 1000, message = "Note must not exceed 1000 characters")
    @Schema(description = "Optional process note", example = "Customer confirmed order details")
    private String note;

    @Schema(description = "Optional deposit override applied before approved requests convert to orders", example = "300000")
    private BigDecimal depositAmount;
}
