package com.vn.sodu.product.service;

import com.vn.sodu.product.Product;
import com.vn.sodu.product.mapper.ProductMapper;
import com.vn.sodu.product.dto.NhanhProductDTO;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class ProductMapperTest {

    private final ProductMapper mapper = new ProductMapper();

    @Test
    void toEntity_mapsFields_andSetsExternalId() {
        NhanhProductDTO dto = new NhanhProductDTO();
        dto.setId(123L);
        dto.setName("Test Product");
        dto.setCode("TP-001");
        dto.setBarcode("1234567890");
        dto.setVat(10);
        dto.setDescription("desc");
        dto.setContent("content");
        dto.setCountryName("VN");
        dto.setCreatedAt(Instant.now().toEpochMilli());
        dto.setUpdatedAt(Instant.now().toEpochMilli());

        Product p = mapper.toEntity(dto);

        assertNotNull(p);
        assertEquals(123L, p.getExternalId());
        assertEquals(123L, p.getId());
        assertEquals("Test Product", p.getName());
        assertEquals("TP-001", p.getCode());
        assertEquals("1234567890", p.getBarcode());
        assertEquals(Integer.valueOf(10), p.getVat());
        assertEquals("desc", p.getDescription());
        assertEquals("content", p.getContent());
        assertEquals("VN", p.getCountryName());
        assertNotNull(p.getCreatedAt());
        assertNotNull(p.getUpdatedAt());
    }
}
