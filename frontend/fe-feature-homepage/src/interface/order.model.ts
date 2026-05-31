import {OrderStatus, OrderSyncStatus, RequestType} from "../enum/union-types";

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
    totalAmount: number;
    depositAmount: number;
    customerName: string;
    customerMobile: string;
    customerAddress: string;
    nhanhOrderId?: string;
    nhanhOrderCode?: string;
    syncError?: string;
    items: OrderItemResponseDto[];
    createdAt?: string;
    updatedAt?: string;
}

export interface OrderSyncResultDto {
    orderId: number;
    orderCode: string;
    syncStatus: OrderSyncStatus;
    nhanhOrderId?: string;
    nhanhOrderCode?: string;
    syncError?: string;
}