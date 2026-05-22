package com.vn.sodu.request.strategy;

import com.vn.sodu.request.Request;
import com.vn.sodu.request.RequestStatus;
import com.vn.sodu.request.dto.CreateRequestDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class PreOrderRequestStrategy implements RequestStrategy {

    private static final int DEPOSIT_PERCENT = 30;

    @Override
    public void validate(CreateRequestDto dto) {
        RequestStrategySupport.requireItems(dto);
    }

    @Override
    public BigDecimal calculateTotal(CreateRequestDto dto) {
        RequestStrategySupport.requireItems(dto);
        return RequestStrategySupport.sumItems(dto.getItems(), false);
    }

    @Override
    public BigDecimal calculateDeposit(CreateRequestDto dto) {
        return RequestStrategySupport.percentageOf(calculateTotal(dto), DEPOSIT_PERCENT);
    }

    @Override
    public RequestStatus initialStatus() {
        return RequestStatus.SOURCING;
    }

    @Override
    public boolean autoCreateNhanhOrder() {
        return false;
    }

    @Override
    public Map<String, Object> toNhanhOrderData(Request request) {
        return RequestStrategySupport.buildDefaultNhanhOrderData(
                new RequestStrategySupport.RequestContext(
                        request.getRequestCode(),
                        request.getCustomerPhone(),
                        request.getType(),
                        request.getStatus(),
                        request.getTotalAmount(),
                        request.getDepositAmount(),
                        request.getItems() == null ? 0 : request.getItems().size()
                )
        );
    }
}
