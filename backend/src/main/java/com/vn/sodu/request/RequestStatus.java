package com.vn.sodu.request;

public enum RequestStatus {
    PENDING,           // Mới tạo - chờ admin
    QUOTED,            // Admin báo giá
    CUSTOMER_CONFIRMED,// Khách confirm giá
    READY_TO_ORDER,    // Sẵn sàng tạo Nhanh order
    ORDER_CREATED,     // Admin đã tạo Nhanh order
    SYNCED,            // Sync thành công từ Nhanh
    PAID_DEPOSIT,      // Đã đặt cọc
    COMPLETED,         // Hoàn thành
    CANCELLED;         // Hủy

    public boolean canCreateNhanhOrder() {
        return this == READY_TO_ORDER || this == CUSTOMER_CONFIRMED;
    }

    public boolean canPayDeposit() {
        return this == ORDER_CREATED || this == SYNCED;
    }
}
