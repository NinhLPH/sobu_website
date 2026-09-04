package com.vn.sodu.voucher;

import com.vn.sodu.voucher.dto.VoucherApplyRequestDto;
import com.vn.sodu.voucher.dto.VoucherApplyResponseDto;
import com.vn.sodu.voucher.dto.VoucherDTO;
import com.vn.sodu.voucher.service.VoucherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = {"integration.nhanh.enabled=false", "spring.sql.init.mode=never"})
public class VoucherSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VoucherService voucherService;

    @Test
    @DisplayName("Admin user with ROLE_ADMIN should access admin voucher endpoints successfully")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminUser_canAccessAdminVoucherEndpoints() throws Exception {
        when(voucherService.getVouchers(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/vouchers")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/vouchers")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Staff user with ROLE_STAFF should access admin voucher endpoints successfully")
    @WithMockUser(username = "staff", roles = {"STAFF"})
    void staffUser_canAccessAdminVoucherEndpoints() throws Exception {
        when(voucherService.getVouchers(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/vouchers")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Normal user with ROLE_USER should be rejected with 403 Forbidden on admin voucher endpoints")
    @WithMockUser(username = "user", roles = {"USER"})
    void normalUser_gets403OnAdminVoucherEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/vouchers")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/vouchers")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Anonymous user should access public voucher endpoints without 403 Forbidden")
    void anonymousUser_canAccessPublicVoucherEndpoints() throws Exception {
        when(voucherService.getActiveVouchers()).thenReturn(List.of());
        when(voucherService.applyVouchers(any())).thenReturn(
                VoucherApplyResponseDto.builder().valid(true).message("Success").build()
        );

        mockMvc.perform(get("/api/v1/vouchers/active")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/vouchers/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subtotal\": 100000, \"shippingFee\": 30000}"))
                .andExpect(status().isOk());
    }
}
