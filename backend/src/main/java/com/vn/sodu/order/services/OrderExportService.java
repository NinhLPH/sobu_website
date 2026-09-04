package com.vn.sodu.order.services;

import com.vn.sodu.order.Order;
import com.vn.sodu.order.OrderItem;
import com.vn.sodu.order.OrderStatus;
import com.vn.sodu.order.repo.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderExportService {

    private final OrderRepository orderRepository;

    private static final String SHEET_NAME = "Tạo đơn (địa chỉ mới)";

    private static final String[] HEADERS = {
            "*Mã đơn hàng", // 1 (Col 0)
            "*Tên người nhận", // 2 (Col 1)
            "*Số điện thoại", // 3 (Col 2)
            "*Tỉnh/Thành Phố", // 4 (Col 3)
            "*Xã/Phường", // 5 (Col 4)
            "*Địa chỉ chi tiết", // 6 (Col 5)
            "Lưu ý về địa chỉ", // 7 (Col 6)
            "Mã bưu chính", // 8 (Col 7)
            "*Tên sản phẩm", // 9 (Col 8)
            "Số lượng (Thông tin bắt buộc khi chọn Giao hàng một phần & Thu COD)", // 10 (Col 9)
            "Giá tiền (Thông tin bắt buộc khi chọn Giao hàng một phần & Thu COD)", // 11 (Col 10)
            "*Tổng cân nặng bưu gửi (KG)", // 12 (Col 11)
            "Chiều dài (CM)", // 13 (Col 12)
            "Chiều rộng (CM)", // 14 (Col 13)
            "Chiều cao (CM)", // 15 (Col 14)
            "Mã khách hàng", // 16 (Col 15)
            "*Giá trị đơn hàng", // 17 (Col 16)
            "*Giao hàng một phần (Y/N)", // 18 (Col 17)
            "*Cho phép thử hàng (Y/N)", // 19 (Col 18)
            "*Cho xem hàng, không cho thử (Y/N)", // 20 (Col 19)
            "Thu phí từ chối nhận hàng (Y/N)", // 21 (Col 20)
            "Phí từ chối nhận hàng cần thu", // 22 (Col 21)
            "*Thu COD (Y/N)", // 23 (Col 22)
            "Số tiền COD", // 24 (Col 23)
            "bưu gửi giá trị cao (Y/N)", // 25 (Col 24)
            "*Hình thức thanh Toán", // 26 (Col 25)
            "Lưu ý giao hàng", // 27 (Col 26)
            "Nhắc nhở điền đúng số tiền COD", // 28 (Col 27)
            "Đơn chỉ hoàn thành nếu ở dưới hiện \"Đủ điều kiện\"" // 29 (Col 28)
    };

    @Transactional(readOnly = true)
    public byte[] exportSpxExcel(List<Long> ids, String statusStr, String query) {
        List<Order> orders;

        if (ids != null && !ids.isEmpty()) {
            orders = orderRepository.findAllById(ids);
        } else {
            OrderStatus status = null;
            if (statusStr != null && !statusStr.isBlank() && !"ALL".equalsIgnoreCase(statusStr)) {
                try {
                    status = OrderStatus.valueOf(statusStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid order status filter: {}", statusStr);
                }
            }
            orders = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
            if (status != null) {
                OrderStatus targetStatus = status;
                orders = orders.stream().filter(o -> o.getStatus() == targetStatus).toList();
            }
            if (query != null && !query.isBlank()) {
                String q = query.trim().toLowerCase();
                orders = orders.stream()
                        .filter(o -> (o.getOrderCode() != null && o.getOrderCode().toLowerCase().contains(q)) ||
                                (o.getCustomerName() != null && o.getCustomerName().toLowerCase().contains(q)) ||
                                (o.getCustomerMobile() != null && o.getCustomerMobile().contains(q)))
                        .toList();
            }
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Header Row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;

            for (Order order : orders) {
                List<OrderItem> items = order.getItems() != null ? order.getItems() : new ArrayList<>();
                String orderCode = order.getOrderCode() != null ? order.getOrderCode() : String.valueOf(order.getId());
                String customerName = order.getCustomerName() != null ? order.getCustomerName() : "";
                String customerMobile = order.getCustomerMobile() != null ? order.getCustomerMobile() : "";

                String provinceName = order.getCustomerCityName() != null ? order.getCustomerCityName() : "";
                String wardName = order.getCustomerWardName() != null ? order.getCustomerWardName() : "";

                String address = order.getCustomerAddress() != null ? order.getCustomerAddress()
                        : (order.getCustomerStreet() != null ? order.getCustomerStreet() : "");

                BigDecimal totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
                BigDecimal remaining = order.getRemainingAmount() != null ? order.getRemainingAmount()
                        : BigDecimal.ZERO;

                // COD Calculation
                boolean hasCod = remaining.compareTo(BigDecimal.ZERO) > 0;
                String codFlag = hasCod ? "Y" : "N";
                double codAmount = hasCod ? remaining.doubleValue() : 0.0;

                // Shipping Payment Method Rule: Default "Người nhận trả", if freeship voucher
                // then "Người gửi trả"
                boolean isFreeship = (order.getShippingVoucherCode() != null
                        && !order.getShippingVoucherCode().isBlank())
                        || (order.getShippingDiscountAmount() != null
                                && order.getShippingDiscountAmount().compareTo(BigDecimal.ZERO) > 0);
                String paymentMethod = isFreeship ? "Người gửi trả" : "Người nhận trả";

                String note = order.getDescription() != null ? order.getDescription() : "";

                if (items.isEmpty()) {
                    Row row = sheet.createRow(rowIndex++);
                    fillOrderRow(row, orderCode, customerName, customerMobile, provinceName, wardName, address,
                            "", 1, 0.0, totalAmount.doubleValue(), codFlag, codAmount, paymentMethod, note);
                } else {
                    for (int i = 0; i < items.size(); i++) {
                        OrderItem item = items.get(i);
                        Row row = sheet.createRow(rowIndex++);
                        String prodName = item.getName() != null ? item.getName() : "";
                        int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                        double price = item.getPrice() != null ? item.getPrice().doubleValue() : 0.0;

                        if (i == 0) {
                            // Primary item row contains all order level fields
                            fillOrderRow(row, orderCode, customerName, customerMobile, provinceName, wardName, address,
                                    prodName, qty, price, totalAmount.doubleValue(), codFlag, codAmount, paymentMethod,
                                    note);
                        } else {
                            // Secondary item rows only repeat Order Code + Item details
                            row.createCell(0).setCellValue(orderCode); // Col 0: *Mã đơn hàng
                            row.createCell(8).setCellValue(prodName); // Col 8: *Tên sản phẩm
                            row.createCell(9).setCellValue(qty); // Col 9: Số lượng (...)
                            row.createCell(10).setCellValue(price); // Col 10: Giá tiền (...)
                        }
                    }
                }
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Error generating SPX Excel export", e);
            throw new RuntimeException("Failed to export SPX Excel file", e);
        }
    }

    private void fillOrderRow(Row row, String orderCode, String name, String phone,
            String provinceName, String wardName, String address,
            String prodName, int qty, double price,
            double orderValue, String codFlag, double codAmount,
            String paymentMethod, String note) {
        row.createCell(0).setCellValue(orderCode); // 1. *Mã đơn hàng
        row.createCell(1).setCellValue(name); // 2. *Tên người nhận
        row.createCell(2).setCellValue(phone); // 3. *Số điện thoại
        row.createCell(3).setCellValue(provinceName); // 4. *Tỉnh/Thành Phố
        row.createCell(4).setCellValue(wardName); // 5. *Xã/Phường
        row.createCell(5).setCellValue(address); // 6. *Địa chỉ chi tiết
        row.createCell(6).setCellValue(""); // 7. Lưu ý về địa chỉ
        row.createCell(7).setCellValue(""); // 8. Mã bưu chính
        row.createCell(8).setCellValue(prodName); // 9. *Tên sản phẩm
        row.createCell(9).setCellValue(qty); // 10. Số lượng (...)
        row.createCell(10).setCellValue(price); // 11. Giá tiền (...)
        row.createCell(11).setCellValue(""); // 12. *Tổng cân nặng bưu gửi (KG)
        row.createCell(12).setCellValue(""); // 13. Chiều dài (CM)
        row.createCell(13).setCellValue(""); // 14. Chiều rộng (CM)
        row.createCell(14).setCellValue(""); // 15. Chiều cao (CM)
        row.createCell(15).setCellValue(""); // 16. Mã khách hàng (set to null/empty)
        row.createCell(16).setCellValue(orderValue); // 17. *Giá trị đơn hàng
        row.createCell(17).setCellValue("N"); // 18. *Giao hàng một phần (Y/N)
        row.createCell(18).setCellValue("N"); // 19. *Cho phép thử hàng (Y/N)
        row.createCell(19).setCellValue("N"); // 20. *Cho xem hàng, không cho thử (Y/N)
        row.createCell(20).setCellValue("N"); // 21. Thu phí từ chối nhận hàng (Y/N)
        row.createCell(21).setCellValue(""); // 22. Phí từ chối nhận hàng cần thu
        row.createCell(22).setCellValue(codFlag); // 23. *Thu COD (Y/N)
        row.createCell(23).setCellValue(codAmount); // 24. Số tiền COD
        row.createCell(24).setCellValue("N"); // 25. bưu gửi giá trị cao (Y/N)
        row.createCell(25).setCellValue(paymentMethod); // 26. *Hình thức thanh Toán
        row.createCell(26).setCellValue(note); // 27. Lưu ý giao hàng
        row.createCell(27).setCellValue(""); // 28. Nhắc nhở điền đúng số tiền COD
        row.createCell(28).setCellValue(""); // 29. Đơn chỉ hoàn thành...
    }
}
