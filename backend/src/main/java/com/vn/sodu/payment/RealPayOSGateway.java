package com.vn.sodu.payment;

import com.vn.sodu.order.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.exception.PayOSException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payos.gateway-mode", havingValue = "real")
public class RealPayOSGateway implements PayOSGateway {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final PayOSProperties properties;

    @Override
    public PayOSCheckoutSession createCheckout(Order order, OrderPayment payment) {
        validateConfig();
        if (payment == null || payment.getProviderOrderCode() == null) {
            throw new IllegalArgumentException("PayOS provider order code is required");
        }

        CreatePaymentLinkRequest request = CreatePaymentLinkRequest.builder()
                .orderCode(payment.getProviderOrderCode())
                .amount(toPayOSAmount(payment.getAmount()))
                .description(buildDescription(payment))
                .returnUrl(properties.getReturnUrl())
                .cancelUrl(properties.getCancelUrl())
                .buyerName(order == null ? null : order.getCustomerName())
                .buyerEmail(order == null ? null : order.getCustomerEmail())
                .buyerPhone(order == null ? null : order.getCustomerMobile())
                .buyerAddress(order == null ? null : order.getCustomerAddress())
                .build();

        try {
            CreatePaymentLinkResponse response = payOS().paymentRequests().create(request);
            return new PayOSCheckoutSession(
                    response.getPaymentLinkId(),
                    response.getCheckoutUrl(),
                    response.getQrCode(),
                    toLocalDateTime(response.getExpiredAt())
            );
        } catch (PayOSException ex) {
            throw new IllegalStateException("PayOS checkout creation failed: " + ex.getMessage(), ex);
        }
    }

    private PayOS payOS() {
        return new PayOS(properties.getClientId(), properties.getApiKey(), properties.getChecksumKey());
    }

    private void validateConfig() {
        if (isBlank(properties.getClientId()) || isBlank(properties.getApiKey()) || isBlank(properties.getChecksumKey())) {
            throw new IllegalStateException("PayOS credentials are required when payos.gateway-mode=real");
        }
        if (isBlank(properties.getReturnUrl()) || isBlank(properties.getCancelUrl())) {
            throw new IllegalStateException("PayOS returnUrl and cancelUrl are required");
        }
    }

    private long toPayOSAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("PayOS amount must be greater than 0");
        }
        return amount.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private String buildDescription(OrderPayment payment) {
        String paymentCode = payment.getPaymentCode() == null ? String.valueOf(payment.getProviderOrderCode()) : payment.getPaymentCode();
        String description = "SOBU " + paymentCode.replace("SOBU-PAY-", "");
        return description.length() <= 25 ? description : description.substring(0, 25);
    }

    private LocalDateTime toLocalDateTime(Long epochSeconds) {
        if (epochSeconds == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), VIETNAM_ZONE);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
