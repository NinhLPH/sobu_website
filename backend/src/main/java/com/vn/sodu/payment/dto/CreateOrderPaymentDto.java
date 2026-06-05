package com.vn.sodu.payment.dto;

import com.vn.sodu.payment.PaymentType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrderPaymentDto {

    @NotNull(message = "Payment type is required")
    private PaymentType type;
}
