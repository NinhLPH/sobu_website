package com.vn.sodu.request.strategy;

import com.vn.sodu.product.repo.ProductRepo;
import com.vn.sodu.request.Request;
import com.vn.sodu.request.RequestStatus;
import com.vn.sodu.request.dto.CreateRequestDto;
import com.vn.sodu.request.dto.RequestItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PreOrderRequestStrategy implements RequestStrategy {

    private static final int DEPOSIT_PERCENT = 30;
    private final ProductRepo productRepo;

    @Override
    public void validate(CreateRequestDto dto) {
        RequestStrategySupport.requireItems(dto);
        for (RequestItemDto item : dto.getItems()) {
            validateExistingCatalogItem(item);
        }
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

    private void validateExistingCatalogItem(RequestItemDto item) {
        if (item == null || item.getNhanhProductId() == null || item.getNhanhProductId().isBlank()) {
            throw new IllegalArgumentException("PREORDER items must include an existing product external id");
        }

        String rawExternalId = item.getNhanhProductId().trim();
        long externalId;
        try {
            externalId = Long.parseLong(rawExternalId);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("PREORDER item external id must be numeric: " + rawExternalId, ex);
        }

        if (productRepo.findByExternalId(externalId).isEmpty()) {
            throw new IllegalArgumentException("PREORDER product not found for external id: " + rawExternalId);
        }
    }
}
