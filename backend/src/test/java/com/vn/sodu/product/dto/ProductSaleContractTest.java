package com.vn.sodu.product.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ProductSaleContractTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void readsAdminSaleValidityFields() throws Exception {
        ProductCreateRequest request = mapper.readValue("""
                {
                  "name": "Sale product",
                  "retailPrice": 80000,
                  "oldPrice": 100000,
                  "saleValidFrom": "2026-08-25T08:00:00",
                  "saleValidThrough": "2026-08-31T23:59:00"
                }
                """, ProductCreateRequest.class);

        assertThat(request.getSaleValidFrom()).isEqualTo(LocalDateTime.of(2026, 8, 25, 8, 0));
        assertThat(request.getSaleValidThrough()).isEqualTo(LocalDateTime.of(2026, 8, 31, 23, 59));
    }

    @Test
    void acceptsSaleFilterAliasFromPostContract() throws Exception {
        ProductFilterRequest request = mapper.readValue("""
                {"sale": true, "sortBy": "discountPercent", "sortDirection": "DESC"}
                """, ProductFilterRequest.class);

        assertThat(request.getOnSale()).isTrue();
        assertThat(request.getSortBy()).isEqualTo("discountPercent");
        assertThat(request.getSortDirection()).isEqualTo("DESC");
    }
}
