import {
    NhanhSyncStage,
    OrderStatus,
    OrderSyncStatus,
    PaymentStatus,
    RequestType
} from "../enum/union-types";

export interface OrderItemResponseDto {
    id: number;
    name: string;
    nhanhProductId?: string;
    note?: string;
    price: number;
    quantity: number;
    discount?: number;
}

export interface OrderResponseDto {
    id: number;
    orderCode: string;
    requestId: number;
    requestCode: string;
    type: RequestType;
    status: OrderStatus;
    syncStatus: OrderSyncStatus;
    nhanhSyncStage: NhanhSyncStage;
    totalAmount: number;
    depositAmount: number;
    shippingFee: number;
    paidAmount: number;
    remainingAmount: number;
    paymentStatus: PaymentStatus;
    customerName: string;
    customerMobile: string;
    customerAddress: string;
    customerCityId?: number;
    customerDistrictId?: number;
    customerWardId?: number;
    nhanhOrderId?: string;
    nhanhOrderCode?: string;
    syncError?: string;
    lastSyncMessage?: string;
    lastSyncAt?: string;
    carrierId?: number;
    carrierServiceId?: number;
    items: OrderItemResponseDto[];
    createdAt?: string;
    updatedAt?: string;
}

export interface OrderSyncResultDto {
    orderId: number;
    orderCode: string;
    syncStatus: OrderSyncStatus;
    nhanhSyncStage: NhanhSyncStage;
    nhanhOrderId?: string;
    nhanhOrderCode?: string;
    syncError?: string;
    lastSyncMessage?: string;
    lastSyncAt?: string;
}
