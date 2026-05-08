package com.vn.sodu.request.dto;

import com.vn.sodu.request.OrderType;
import com.vn.sodu.request.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RequestResponseDto {

    @Schema(description = "Request identifier")
    private Long id;

    @Schema(description = "Request code")
    private String requestCode;

    @Schema(description = "Customer phone")
    private String customerPhone;

    @Schema(description = "Request type")
    private OrderType type;

    @Schema(description = "Current request status")
    private RequestStatus status;

    @Schema(description = "Total amount")
    private BigDecimal totalAmount;

    @Schema(description = "Deposit amount")
    private BigDecimal depositAmount;

    @Schema(description = "Custom requirements JSON/text")
    private String customRequirements;

    @Schema(description = "Nhanh order id")
    private String nhanhOrderId;

    @Schema(description = "Nhanh order code")
    private String nhanhOrderCode;

    @Schema(description = "Request items")
    private List<RequestItemResponseDto> items;

    @Schema(description = "Request attachments")
    private List<RequestAttachmentDto> attachments;

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Updated timestamp")
    private LocalDateTime updatedAt;
}
