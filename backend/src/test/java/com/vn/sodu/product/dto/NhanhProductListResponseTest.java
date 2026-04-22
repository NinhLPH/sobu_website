package com.vn.sodu.product.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NhanhProductListResponseTest {

    @Test
    public void testParseJsonSuccess() throws Exception {
        String json = """
                {
                    "code": 1,
                    "data": {
                        "products": [
                            {
                                "id": 1,
                                "name": "Product 1",
                                "code": "P001"
                            }
                        ],
                        "page": 1,
                        "totalPages": 5
                    }
                }
                """;

        ObjectMapper mapper = new ObjectMapper();
        NhanhProductListResponse response = mapper.readValue(json, NhanhProductListResponse.class);

        assertEquals(1, response.getCode());
        assertNotNull(response.getData());
        assertNotNull(response.getData().getProducts());
        assertEquals(1, response.getData().getProducts().size());
        assertEquals(1, response.getData().getPage());
        assertEquals(5, response.getData().getTotalPages());
    }

    @Test
    public void testProductsNotNull() throws Exception {
        String json = """
                {
                    "code": 1,
                    "data": {
                        "products": [],
                        "page": 1,
                        "totalPages": 1
                    }
                }
                """;

        ObjectMapper mapper = new ObjectMapper();
        NhanhProductListResponse response = mapper.readValue(json, NhanhProductListResponse.class);

        assertNotNull(response.getData().getProducts());
        assertTrue(response.getData().getProducts().isEmpty());
    }

    @Test
    public void testIgnoreUnknownProperties() throws Exception {
        String json = """
                {
                    "code": 1,
                    "data": {
                        "products": [],
                        "page": 1,
                        "totalPages": 1,
                        "unknownField": "ignored"
                    },
                    "unknownRootField": "also ignored"
                }
                """;

        ObjectMapper mapper = new ObjectMapper();
        NhanhProductListResponse response = mapper.readValue(json, NhanhProductListResponse.class);

        assertNotNull(response);
        assertEquals(1, response.getCode());
    }
}
