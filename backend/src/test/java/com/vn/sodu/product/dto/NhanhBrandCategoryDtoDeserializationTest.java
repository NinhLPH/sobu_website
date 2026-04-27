package com.vn.sodu.product.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.product.brand.dto.NhanhBrandDTO;
import com.vn.sodu.product.category.dto.NhanhCategoryDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NhanhBrandCategoryDtoDeserializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldDeserializeBrandDtoWithUnknownAndNullFields() throws Exception {
        String json = """
                {
                  "id": 1,
                  "parentId": null,
                  "code": "B001",
                  "name": null,
                  "status": 1,
                  "createdAt": null,
                  "unknownField": "ignored"
                }
                """;

        NhanhBrandDTO dto = mapper.readValue(json, NhanhBrandDTO.class);

        assertEquals(1L, dto.getId());
        assertNull(dto.getParentId());
        assertNull(dto.getName());
        assertNull(dto.getCreatedAt());
    }

    @Test
    void shouldDeserializeCategoryDtoWithUnknownAndNullFields() throws Exception {
        String json = """
                {
                  "id": 10,
                  "parentId": null,
                  "code": "C001",
                  "name": "Category",
                  "order": null,
                  "image": null,
                  "content": null,
                  "status": 1,
                  "unknownField": "ignored"
                }
                """;

        NhanhCategoryDTO dto = mapper.readValue(json, NhanhCategoryDTO.class);

        assertEquals(10L, dto.getId());
        assertNull(dto.getParentId());
        assertNull(dto.getOrder());
        assertNull(dto.getImage());
        assertNull(dto.getContent());
    }
}
