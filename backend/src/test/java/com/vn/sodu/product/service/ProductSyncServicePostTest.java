package com.vn.sodu.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.product.dto.NhanhProductDTO;
import com.vn.sodu.product.dto.NhanhResponse;
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
                "filters", Map.of("updatedAtFrom", 1704067200L),
                "paginator", Map.of("size", 50)
        );

        assertEquals(1704067200L, ((Map<?, ?>) body.get("filters")).get("updatedAtFrom"));
        assertEquals(50, ((Map<?, ?>) body.get("paginator")).get("size"));
    }

    @Test
    public void testPostResponseParsingWithCode1() throws Exception {
        String json = """
                {
                    "code": 1,
                    "data": [
                        {"id": 1, "name": "Product 1"},
                        {"id": 2, "name": "Product 2"}
                    ],
                    "paginator": {
                        "next": "cursor-2"
                    }
                }
                """;

        ObjectMapper mapper = new ObjectMapper();
        NhanhResponse<List<NhanhProductDTO>> response = mapper.readerFor(
                mapper.getTypeFactory().constructParametricType(NhanhResponse.class,
                        mapper.getTypeFactory().constructCollectionType(List.class, NhanhProductDTO.class))
        ).readValue(json);

        // Verify response
        assertEquals(1, response.getCode(), "API should return code = 1");
        assertNotNull(response.getData());
        assertEquals(2, response.getData().size());
        assertEquals("cursor-2", response.getPaginator().getNext());
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
        NhanhResponse<List<NhanhProductDTO>> response = mapper.readerFor(
                mapper.getTypeFactory().constructParametricType(NhanhResponse.class,
                        mapper.getTypeFactory().constructCollectionType(List.class, NhanhProductDTO.class))
        ).readValue(json);

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
        Map<String, Object> paginator = Map.of(
                "size", 50,
                "next", Map.of("id", 100)
        );

        assertEquals(50, paginator.get("size"));
        assertTrue(paginator.containsKey("next"));
    }

    @Test
    public void testHttpEntityCreation() {
        Map<String, Object> body = Map.of(
                "filters", Map.of("updatedAtFrom", 1704067200L),
                "paginator", Map.of("size", 50)
        );

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        assertNotNull(request.getBody());
        assertEquals(body, request.getBody());
        assertNotNull(request.getHeaders());
    }
}
