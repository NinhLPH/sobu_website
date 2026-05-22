package com.vn.sodu.request.normalize;

import com.vn.sodu.request.OrderType;
import com.vn.sodu.request.dto.CreateRequestDto;
import com.vn.sodu.request.dto.RequestItemDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class RequestNormalizerTest {

    private final RequestNormalizer normalizer = new RequestNormalizer();

    @Test
    void normalizesCreateRequestByTrimmingAndMergingItems() {
        CreateRequestDto dto = CreateRequestDto.builder()
                .customerPhone("  0123456789  ")
                .type(OrderType.NORMAL)
                .customRequirements("  <p> Need  custom   note </p>  ")
                .uploadedImageUrls(Arrays.asList(" https://img/1 ", "https://img/1", " ", null, "https://img/2"))
                .items(List.of(
                        RequestItemDto.builder()
                                .nhanhProductId("  P-1 ")
                                .name("  Product A  ")
                                .note("  note  ")
                                .price(new BigDecimal("100"))
                                .quantity(1)
                                .build(),
                        RequestItemDto.builder()
                                .nhanhProductId("P-1")
                                .name("Product A")
                                .note("note")
                                .price(new BigDecimal("100"))
                                .quantity(2)
                                .build(),
                        RequestItemDto.builder()
                                .name("   ")
                                .quantity(10)
                                .build(),
                        RequestItemDto.builder()
                                .name("Product B")
                                .quantity(0)
                                .build()
                ))
                .build();

        CreateRequestDto normalized = normalizer.normalize(dto);

        assertThat(normalized.getCustomerPhone()).isEqualTo("0123456789");
        assertThat(normalized.getCustomRequirements()).isEqualTo("Need custom note");
        assertThat(normalized.getUploadedImageUrls()).containsExactly("https://img/1", "https://img/2");
        assertThat(normalized.getItems()).hasSize(1);
        assertThat(normalized.getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(normalized.getItems().get(0).getName()).isEqualTo("Product A");
    }

    @Test
    void returnsEmptyCollectionsForNullLists() {
        CreateRequestDto dto = CreateRequestDto.builder()
                .customerPhone("0123")
                .type(OrderType.CUSTOM)
                .items(null)
                .build();

        CreateRequestDto normalized = normalizer.normalize(dto);

        assertThat(normalized.getItems()).isEmpty();
        assertThat(normalized.getUploadedImageUrls()).isEmpty();
    }

    @Test
    void skipsInvalidItemsAndDeduplicatesByProductId() {
        CreateRequestDto dto = CreateRequestDto.builder()
                .customerPhone("0123")
                .type(OrderType.PREORDER)
                .customRequirements("requirement")
                .items(List.of(
                        RequestItemDto.builder()
                                .nhanhProductId("P1")
                                .name("Item")
                                .quantity(1)
                                .build(),
                        RequestItemDto.builder()
                                .nhanhProductId("P1")
                                .name("Item")
                                .quantity(2)
                                .build(),
                        RequestItemDto.builder()
                                .name("Bad")
                                .quantity(-1)
                                .build()
                ))
                .uploadedImageUrls(List.of("a", "a", "b", "  "))
                .build();

        CreateRequestDto normalized = normalizer.normalize(dto);

        assertThat(normalized.getItems()).hasSize(1);
        assertThat(normalized.getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(normalized.getUploadedImageUrls()).containsExactly("a", "b");
    }
}
