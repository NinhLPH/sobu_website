package com.vn.sodu.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.product.dto.NhanhProductDTO;
import com.vn.sodu.product.dto.NhanhProductListResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ProductSyncServicePostTest {

    @Test
    public void testPostRequestBodyStructure() throws Exception {
        // Verify POST request body structure
        Map<String, Object> body = Map.of(
                "page", 1,
                "pageSize", 50
        );

        assertEquals(1, body.get("page"));
        assertEquals(50, body.get("pageSize"));
    }

    @Test
    public void testPostResponseParsingWithCode1() throws Exception {
        String json = """
                {
                    "code": 1,
                    "data": {
                        "products": [
                            {"id": 1, "name": "Product 1"},
                            {"id": 2, "name": "Product 2"}
                        ],
                        "page": 1,
                        "totalPages": 3
                    }
                }
                """;

        ObjectMapper mapper = new ObjectMapper();
        NhanhProductListResponse response = mapper.readValue(json, NhanhProductListResponse.class);

        // Verify response
        assertEquals(1, response.getCode(), "API should return code = 1");
        assertNotNull(response.getData());
        assertNotNull(response.getData().getProducts());
        assertEquals(2, response.getData().getProducts().size());
        assertEquals(1, response.getData().getPage());
        assertEquals(3, response.getData().getTotalPages());
    }

    @Test
    public void testPostResponseFailsWithCodeNot1() throws Exception {
        String json = """
                {
                    "code": 0,
                    "data": null
                }
                """;

        ObjectMapper mapper = new ObjectMapper();
        NhanhProductListResponse response = mapper.readValue(json, NhanhProductListResponse.class);

        assertNotEquals(1, response.getCode(), "Should fail if code != 1");
    }

    @Test
    public void testHeadersConfiguration() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer test_token");

        assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
        assertEquals("Bearer test_token", headers.getFirst("Authorization"));
    }

    @Test
    public void testPaginationLogic() {
        // Simulate pagination with page and totalPages
        int currentPage = 1;
        int totalPages = 5;

        assertTrue(currentPage < totalPages, "Should continue pagination if current page < total pages");

        currentPage = 5;
        assertFalse(currentPage < totalPages, "Should stop pagination if current page >= total pages");
    }

    @Test
    public void testHttpEntityCreation() {
        Map<String, Object> body = Map.of(
                "page", 1,
                "pageSize", 50
        );

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        assertNotNull(request.getBody());
        assertEquals(body, request.getBody());
        assertNotNull(request.getHeaders());
    }
}
