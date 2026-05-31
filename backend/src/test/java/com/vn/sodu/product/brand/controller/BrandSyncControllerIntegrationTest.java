package com.vn.sodu.product.brand.controller;

import com.vn.sodu.product.brand.service.BrandSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class BrandSyncControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BrandSyncService brandSyncService;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminUserCanCallSyncEndpoint() throws Exception {
        doNothing().when(brandSyncService).syncBrands();

        mockMvc.perform(post("/api/admin/brands/sync")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(brandSyncService).syncBrands();
    }
}
