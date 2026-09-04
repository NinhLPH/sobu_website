package com.vn.sodu.inventory.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.global.dto.PageResponse;
import com.vn.sodu.inventory.InventoryAdjustmentType;
import com.vn.sodu.inventory.InventoryService;
import com.vn.sodu.inventory.dto.InventoryAdjustmentDto;
import com.vn.sodu.inventory.dto.InventoryAdjustmentRequest;
import com.vn.sodu.inventory.dto.InventoryBalanceDto;
import com.vn.sodu.inventory.dto.InventoryProductDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminInventoryControllerTest {

    @Test
    @DisplayName("Should allow staff to set opening stock")
    void setOpeningStockAllowsStaff() {
        InventoryService service = mock(InventoryService.class);
        AdminInventoryController controller = new AdminInventoryController(service);

        AdminInventoryController.OpeningStockRequest request = new AdminInventoryController.OpeningStockRequest();
        request.setQuantity(10.0);
        request.setNote("Initial stock");

        InventoryAdjustmentDto dto = InventoryAdjustmentDto.builder()
                .id(1L).productId(5L).type(InventoryAdjustmentType.OPENING_STOCK)
                .quantityDelta(10.0).balanceAfter(10.0).build();
        when(service.setOpeningStock(5L, 10.0, "Initial stock")).thenReturn(dto);

        var response = controller.setOpeningStock(staffAuth(), 5L, request);

        verify(service).setOpeningStock(5L, 10.0, "Initial stock");
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().getData()).isEqualTo(dto);
    }

    @Test
    @DisplayName("Should reject non-staff for opening stock")
    void setOpeningStockRejectsNonStaff() {
        InventoryService service = mock(InventoryService.class);
        AdminInventoryController controller = new AdminInventoryController(service);

        AdminInventoryController.OpeningStockRequest request = new AdminInventoryController.OpeningStockRequest();
        request.setQuantity(10.0);

        assertThatThrownBy(() -> controller.setOpeningStock(userAuth(), 5L, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Should allow staff to record an adjustment")
    void adjustAllowsStaff() {
        InventoryService service = mock(InventoryService.class);
        AdminInventoryController controller = new AdminInventoryController(service);

        InventoryAdjustmentRequest request = InventoryAdjustmentRequest.builder()
                .type(InventoryAdjustmentType.STOCK_IN)
                .quantity(5.0)
                .note("Received goods")
                .build();
        InventoryAdjustmentDto dto = InventoryAdjustmentDto.builder()
                .id(2L).productId(5L).type(InventoryAdjustmentType.STOCK_IN)
                .quantityDelta(5.0).balanceAfter(15.0).build();
        when(service.adjust(5L, InventoryAdjustmentType.STOCK_IN, 5.0, "Received goods")).thenReturn(dto);

        var response = controller.adjust(staffAuth(), 5L, request);

        verify(service).adjust(5L, InventoryAdjustmentType.STOCK_IN, 5.0, "Received goods");
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().getData()).isEqualTo(dto);
    }

    @Test
    @DisplayName("Should reject non-staff for adjustment")
    void adjustRejectsNonStaff() {
        InventoryService service = mock(InventoryService.class);
        AdminInventoryController controller = new AdminInventoryController(service);

        InventoryAdjustmentRequest request = InventoryAdjustmentRequest.builder()
                .type(InventoryAdjustmentType.STOCK_IN).quantity(5.0).build();

        assertThatThrownBy(() -> controller.adjust(userAuth(), 5L, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Should allow staff to read balance and ledger")
    void readEndpointsAllowStaff() {
        InventoryService service = mock(InventoryService.class);
        AdminInventoryController controller = new AdminInventoryController(service);

        InventoryBalanceDto balance = InventoryBalanceDto.builder()
                .productId(5L).stockRemain(15.0).stockAvailable(15.0).reserved(0.0).build();
        when(service.getBalance(5L)).thenReturn(balance);
        when(service.getLedger(5L)).thenReturn(List.of(
                InventoryAdjustmentDto.builder().id(2L).productId(5L).type(InventoryAdjustmentType.STOCK_IN)
                        .quantityDelta(5.0).balanceAfter(15.0).build()
        ));

        ApiResponseDTO<InventoryBalanceDto> balanceResponse =
                controller.getBalance(staffAuth(), 5L).getBody();
        ApiResponseDTO<List<InventoryAdjustmentDto>> ledgerResponse =
                controller.getLedger(staffAuth(), 5L).getBody();

        assertThat(balanceResponse.getData().getReserved()).isEqualTo(0.0);
        assertThat(ledgerResponse.getData()).hasSize(1);
        assertThat(ledgerResponse.getData().get(0).getType()).isEqualTo(InventoryAdjustmentType.STOCK_IN);
    }

    @Test
    @DisplayName("Should reject non-staff for balance and ledger reads")
    void readEndpointsRejectNonStaff() {
        InventoryService service = mock(InventoryService.class);
        AdminInventoryController controller = new AdminInventoryController(service);

        assertThatThrownBy(() -> controller.getBalance(userAuth(), 5L))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.getLedger(userAuth(), 5L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Should allow staff to query inventory products")
    void getInventoryProductsAllowsStaff() {
        InventoryService service = mock(InventoryService.class);
        AdminInventoryController controller = new AdminInventoryController(service);

        InventoryProductDto item = InventoryProductDto.builder()
                .id(1L).productId(1L).name("Sữa rửa mặt").code("SRM-01")
                .stockRemain(20.0).stockAvailable(15.0).reserved(5.0)
                .build();
        PageResponse<InventoryProductDto> pageResponse = PageResponse.<InventoryProductDto>builder()
                .content(List.of(item))
                .pageNumber(0)
                .pageSize(20)
                .totalElements(1)
                .totalPages(1)
                .build();
        when(service.getInventoryProducts("SRM", "IN_STOCK", 0, 20, "id", "DESC"))
                .thenReturn(pageResponse);

        var response = controller.getInventoryProducts(
                staffAuth(), "SRM", "IN_STOCK", 0, 20, "id", "DESC");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().getContent()).hasSize(1);
        assertThat(response.getBody().getData().getContent().get(0).getReserved()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("Should reject non-staff for inventory products query")
    void getInventoryProductsRejectsNonStaff() {
        InventoryService service = mock(InventoryService.class);
        AdminInventoryController controller = new AdminInventoryController(service);

        assertThatThrownBy(() -> controller.getInventoryProducts(
                userAuth(), null, null, 0, 20, "id", "DESC"))
                .isInstanceOf(AccessDeniedException.class);
    }

    private TestingAuthenticationToken staffAuth() {
        return new TestingAuthenticationToken("staff@sobu.vn", null, "ROLE_STAFF");
    }

    private TestingAuthenticationToken userAuth() {
        return new TestingAuthenticationToken("user@sobu.vn", null, "ROLE_USER");
    }
}