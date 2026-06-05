package com.vn.sodu.request.dto;

import com.vn.sodu.request.OrderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class UpdateRequestDto {

    @Size(max = 20, message = "Customer phone must not exceed 20 characters")
    private String customerPhone;

    private OrderType type;

    @Valid
    private List<RequestItemDto> items;

    @Size(max = 5000, message = "Custom requirements must not exceed 5000 characters")
    private String customRequirements;

    @PositiveOrZero(message = "Total amount must be greater than or equal to 0")
    private BigDecimal totalAmount;

    @PositiveOrZero(message = "Deposit amount must be greater than or equal to 0")
    private BigDecimal depositAmount;

    @Size(max = 50, message = "Uploaded images must not exceed 50 entries")
    private List<@Size(max = 500, message = "Image URL must not exceed 500 characters") String> uploadedImageUrls;
}
