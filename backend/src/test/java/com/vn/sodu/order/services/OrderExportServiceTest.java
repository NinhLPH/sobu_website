package com.vn.sodu.order.services;

import com.vn.sodu.order.Order;
import com.vn.sodu.order.OrderItem;
import com.vn.sodu.order.OrderStatus;
import com.vn.sodu.order.repo.OrderRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderExportServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderExportService orderExportService;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        OrderItem item1 = OrderItem.builder()
                .id(1L)
                .name("Áo Sơ Mi Nam")
                .quantity(2)
                .price(new BigDecimal("250000"))
                .build();

        OrderItem item2 = OrderItem.builder()
                .id(2L)
                .name("Quần Tây Nam")
                .quantity(1)
                .price(new BigDecimal("400000"))
                .build();

        sampleOrder = Order.builder()
                .id(100L)
                .orderCode("ORD100")
                .customerName("Nguyễn Văn A")
                .customerMobile("0987654321")
                .customerCityName("Thành phố Hà Nội")
                .customerDistrictName(null)
                .customerWardName("Phường Hoàn Kiếm")
                .customerAddress("123 đường Xuân Thủy")
                .customerEmail("khach@example.com")
                .totalAmount(new BigDecimal("900000"))
                .remainingAmount(new BigDecimal("900000"))
                .status(OrderStatus.PROCESSING)
                .items(List.of(item1, item2))
                .build();
    }

    @Test
    void exportSpxExcel_ByIds_ReturnsValidXlsx() throws Exception {
        when(orderRepository.findAllById(anyList())).thenReturn(List.of(sampleOrder));

        byte[] result = orderExportService.exportSpxExcel(List.of(100L), null, null);

        assertNotNull(result);
        assertTrue(result.length > 0);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = workbook.getSheet("Tạo đơn (địa chỉ mới)");
            assertNotNull(sheet);

            // Header row
            Row headerRow = sheet.getRow(0);
            assertEquals(29, headerRow.getLastCellNum());
            assertEquals("*Mã đơn hàng", headerRow.getCell(0).getStringCellValue());
            assertEquals("*Tên người nhận", headerRow.getCell(1).getStringCellValue());
            assertEquals("*Tỉnh/Thành Phố", headerRow.getCell(3).getStringCellValue());
            assertEquals("*Xã/Phường", headerRow.getCell(4).getStringCellValue());
            assertEquals("Mã khách hàng", headerRow.getCell(15).getStringCellValue());
            assertEquals("*Giá trị đơn hàng", headerRow.getCell(16).getStringCellValue());
            assertEquals("*Giao hàng một phần (Y/N)", headerRow.getCell(17).getStringCellValue());
            assertEquals("*Cho phép thử hàng (Y/N)", headerRow.getCell(18).getStringCellValue());
            assertEquals("*Cho xem hàng, không cho thử (Y/N)", headerRow.getCell(19).getStringCellValue());
            assertEquals("Thu phí từ chối nhận hàng (Y/N)", headerRow.getCell(20).getStringCellValue());
            assertEquals("*Thu COD (Y/N)", headerRow.getCell(22).getStringCellValue());
            assertEquals("Số tiền COD", headerRow.getCell(23).getStringCellValue());
            assertEquals("bưu gửi giá trị cao (Y/N)", headerRow.getCell(24).getStringCellValue());
            assertEquals("*Hình thức thanh Toán", headerRow.getCell(25).getStringCellValue());

            // Row 1: Order level + Item 1
            Row row1 = sheet.getRow(1);
            assertEquals("ORD100", row1.getCell(0).getStringCellValue());
            assertEquals("Nguyễn Văn A", row1.getCell(1).getStringCellValue());
            assertEquals("0987654321", row1.getCell(2).getStringCellValue());
            assertEquals("Thành phố Hà Nội", row1.getCell(3).getStringCellValue());
            assertEquals("Phường Hoàn Kiếm", row1.getCell(4).getStringCellValue());
            assertEquals("123 đường Xuân Thủy", row1.getCell(5).getStringCellValue());
            assertEquals("Áo Sơ Mi Nam", row1.getCell(8).getStringCellValue());
            assertEquals(2, (int) row1.getCell(9).getNumericCellValue());
            assertEquals(250000.0, row1.getCell(10).getNumericCellValue());
            assertEquals("", row1.getCell(15).getStringCellValue()); // Customer code is null
            assertEquals(900000.0, row1.getCell(16).getNumericCellValue()); // Order value
            assertEquals("N", row1.getCell(17).getStringCellValue()); // Giao hàng 1 phần
            assertEquals("N", row1.getCell(18).getStringCellValue()); // Cho thử hàng
            assertEquals("N", row1.getCell(19).getStringCellValue()); // Cho xem hàng
            assertEquals("N", row1.getCell(20).getStringCellValue()); // Thu phí từ chối
            assertEquals("Y", row1.getCell(22).getStringCellValue()); // Thu COD
            assertEquals(900000.0, row1.getCell(23).getNumericCellValue()); // COD amount for buyer
            assertEquals("N", row1.getCell(24).getStringCellValue()); // Bưu gửi giá trị cao
            assertEquals("Người nhận trả", row1.getCell(25).getStringCellValue()); // Payment method

            // Row 2: Secondary item row
            Row row2 = sheet.getRow(2);
            assertEquals("ORD100", row2.getCell(0).getStringCellValue());
            assertEquals("Quần Tây Nam", row2.getCell(8).getStringCellValue());
            assertEquals(1, (int) row2.getCell(9).getNumericCellValue());
            assertEquals(400000.0, row2.getCell(10).getNumericCellValue());
        }
    }
}
