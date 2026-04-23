package com.vn.sodu.product.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class NhanhProductListResponseTest {

    @Test
    public void testParseJsonSuccess() throws Exception {
        String json = """
                {
                    "code": 1,
                    "data": [
                        {
                            "id": 1,
                            "name": "Product 1",
                            "code": "P001"
                        }
                    ],
                    "paginator": {
                        "next": {
                            "id": 100
                        }
                    }
                }
                """;

        ObjectMapper mapper = new ObjectMapper();
        NhanhResponse<List<NhanhProductDTO>> response = mapper.readerFor(
                mapper.getTypeFactory().constructParametricType(NhanhResponse.class,
                        mapper.getTypeFactory().constructCollectionType(List.class, NhanhProductDTO.class))
        ).readValue(json);

        assertEquals(1, response.getCode());
        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        assertNotNull(response.getPaginator());
        assertInstanceOf(Map.class, response.getPaginator().getNext());
    }

    @Test
    public void testProductsNotNull() throws Exception {
        String json = """
                {
                    "code": 1,
                    "data": []
                }
                """;

        ObjectMapper mapper = new ObjectMapper();
        NhanhResponse<List<NhanhProductDTO>> response = mapper.readerFor(
                mapper.getTypeFactory().constructParametricType(NhanhResponse.class,
                        mapper.getTypeFactory().constructCollectionType(List.class, NhanhProductDTO.class))
        ).readValue(json);

        assertNotNull(response.getData());
        assertTrue(response.getData().isEmpty());
    }

    @Test
    public void testIgnoreUnknownProperties() throws Exception {
        String json = """
                {
                    "code": 1,
                    "data": [],
                    "paginator": {
                        "next": "cursor-2"
                    },
                    "unknownRootField": "ignored"
                }
                """;

        ObjectMapper mapper = new ObjectMapper();
        NhanhResponse<List<NhanhProductDTO>> response = mapper.readerFor(
                mapper.getTypeFactory().constructParametricType(NhanhResponse.class,
                        mapper.getTypeFactory().constructCollectionType(List.class, NhanhProductDTO.class))
        ).readValue(json);

        assertNotNull(response);
        assertEquals(1, response.getCode());
        assertEquals("cursor-2", response.getPaginator().getNext());
    }
}
