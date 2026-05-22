package com.vn.sodu.request.strategy;

import com.vn.sodu.request.OrderType;
import com.vn.sodu.request.Request;
import com.vn.sodu.request.RequestStatus;
import com.vn.sodu.request.dto.CreateRequestDto;
import com.vn.sodu.request.dto.RequestItemDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestStrategyTest {

    private final NormalRequestStrategy normal = new NormalRequestStrategy();
    private final PreOrderRequestStrategy preOrder = new PreOrderRequestStrategy();
    private final FindingRequestStrategy finding = new FindingRequestStrategy();
    private final CustomRequestStrategy custom = new CustomRequestStrategy();

    @Test
    void normalStrategyValidatesAndCalculatesTotal() {
        CreateRequestDto dto = baseDto(OrderType.NORMAL, "normal requirements");

        normal.validate(dto);
        assertThat(normal.calculateTotal(dto)).isEqualByComparingTo("250.00");
        assertThat(normal.calculateDeposit(dto)).isEqualByComparingTo("0.00");
        assertThat(normal.initialStatus()).isEqualTo(RequestStatus.REVIEWING);
        assertThat(normal.autoCreateNhanhOrder()).isTrue();
    }

    @Test
    void preorderStrategyAllowsQuotedOrUnquotedItems() {
        CreateRequestDto dto = baseDto(OrderType.PREORDER, null);

        preOrder.validate(dto);
        assertThat(preOrder.calculateTotal(dto)).isEqualByComparingTo("250.00");
        assertThat(preOrder.calculateDeposit(dto)).isEqualByComparingTo("75.00");
        assertThat(preOrder.initialStatus()).isEqualTo(RequestStatus.SOURCING);
        assertThat(preOrder.autoCreateNhanhOrder()).isFalse();
    }

    @Test
    void findingStrategyRequiresCustomRequirements() {
        CreateRequestDto dto = baseDto(OrderType.FINDING, "Find this product");

        finding.validate(dto);
        assertThat(finding.calculateTotal(dto)).isEqualByComparingTo("0.00");
        assertThat(finding.calculateDeposit(dto)).isEqualByComparingTo("0.00");
        assertThat(finding.initialStatus()).isEqualTo(RequestStatus.SOURCING);
    }

    @Test
    void findingStrategyRejectsMissingRequirements() {
        CreateRequestDto dto = baseDto(OrderType.FINDING, null);

        assertThrows(IllegalArgumentException.class, () -> finding.validate(dto));
    }

    @Test
    void customStrategyRequiresRequirementsAndCalculatesTotalFromProvidedPrices() {
        CreateRequestDto dto = baseDto(OrderType.CUSTOM, "Custom design");

        custom.validate(dto);
        assertThat(custom.calculateTotal(dto)).isEqualByComparingTo("250.00");
        assertThat(custom.calculateDeposit(dto)).isEqualByComparingTo("0.00");
        assertThat(custom.initialStatus()).isEqualTo(RequestStatus.REVIEWING);
    }

    @Test
    void customStrategyRejectsMissingRequirements() {
        CreateRequestDto dto = baseDto(OrderType.CUSTOM, null);

        assertThrows(IllegalArgumentException.class, () -> custom.validate(dto));
    }

    @Test
    void normalStrategyRejectsMissingPrice() {
        CreateRequestDto dto = CreateRequestDto.builder()
                .customerPhone("0123456789")
                .type(OrderType.NORMAL)
                .customRequirements("req")
                .items(List.of(
                        RequestItemDto.builder()
                                .name("Item")
                                .quantity(1)
                                .build()
                ))
                .build();

        assertThrows(IllegalArgumentException.class, () -> normal.validate(dto));
    }

    @Test
    void toNhanhOrderDataReturnsSafeMap() {
        Request request = Request.builder()
                .requestCode("SOBU-REQ-20260507210000-0001")
                .customerPhone("0123")
                .type(OrderType.NORMAL)
                .status(RequestStatus.REVIEWING)
                .totalAmount(new BigDecimal("250.00"))
                .depositAmount(new BigDecimal("0.00"))
                .build();

        assertThat(normal.toNhanhOrderData(request))
                .containsEntry("requestCode", "SOBU-REQ-20260507210000-0001")
                .containsEntry("type", "NORMAL")
                .containsEntry("status", "REVIEWING");
    }

    private CreateRequestDto baseDto(OrderType type, String requirements) {
        return CreateRequestDto.builder()
                .customerPhone("0123456789")
                .type(type)
                .customRequirements(requirements)
                .items(List.of(
                        RequestItemDto.builder()
                                .nhanhProductId("P1")
                                .name("Item 1")
                                .note("note")
                                .price(new BigDecimal("100"))
                                .quantity(2)
                                .build(),
                        RequestItemDto.builder()
                                .nhanhProductId("P2")
                                .name("Item 2")
                                .note("note")
                                .price(new BigDecimal("50"))
                                .quantity(1)
                                .build()
                ))
                .uploadedImageUrls(List.of("https://img/1"))
                .build();
    }
}
