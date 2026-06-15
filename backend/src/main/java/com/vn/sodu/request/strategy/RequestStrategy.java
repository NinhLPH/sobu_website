package com.vn.sodu.request.strategy;

import com.vn.sodu.request.Request;
import com.vn.sodu.request.RequestStatus;
import com.vn.sodu.request.dto.CreateRequestDto;

import java.math.BigDecimal;
import java.util.Map;

public interface RequestStrategy {

    void validate(CreateRequestDto dto);

    BigDecimal calculateTotal(CreateRequestDto dto);

    BigDecimal calculateDeposit(CreateRequestDto dto);

    RequestStatus initialStatus();

    boolean autoCreateNhanhOrder();

    Map<String, Object> toNhanhOrderData(Request request);
}
