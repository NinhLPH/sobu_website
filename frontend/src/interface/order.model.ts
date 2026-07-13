import {
    NhanhSyncStage,
    OrderStatus,
    OrderSyncStatus,
    PaymentMethod,
    PaymentStatus,
    RequestType
} from "../enum/union-types";

export type OrderPaymentType = 'FULL' | 'DEPOSIT' | 'FINAL' | 'REFUND';

export interface CreateNormalOrderItemDto {
    nhanhProductId: string;
    name: string;
    note?: string;
    price: number;
    discount?: number;
    quantity: number;
}

export interface OrderShippingLocationDto {
    customerCityName: string;
    customerDistrictName: string;
    customerWardName: string;
    customerCityId: number;
    customerDistrictId: number;
    customerWardId: number;
}

export interface CreateNormalOrderDto extends Partial<OrderShippingLocationDto> {
    customerName: string;
    customerMobile: string;
    customerEmail?: string;
    customerAddress?: string;
    carrierId?: number;
    carrierServiceId?: number;
    shippingFee?: number;
    description?: string;
    items: CreateNormalOrderItemDto[];
}

export interface OrderItemResponseDto {
    id: number;
    name: string;
    nhanhProductId?: string;
    productId?: number;
    note?: string;
    price: number;
    quantity: number;
    discount?: number;
}

export interface OrderResponseDto extends Partial<OrderShippingLocationDto> {
    id: number;
    orderCode?: string;
    requestId?: number | null;
    requestCode?: string | null;
    type?: RequestType;
    status?: OrderStatus;
    syncStatus?: OrderSyncStatus;
    nhanhSyncStage?: NhanhSyncStage;
    totalAmount?: number;
    depositAmount?: number;
    shippingFee?: number;
    paidAmount?: number;
    remainingAmount?: number;
    paymentStatus?: PaymentStatus;
    customerName?: string;
    customerMobile?: string;
    customerEmail?: string;
    customerAddress?: string;
    nhanhOrderId?: string;
    nhanhOrderCode?: string;
    syncError?: string;
    lastSyncMessage?: string;
    lastSyncAt?: string;
    carrierId?: number;
    carrierServiceId?: number;
    description?: string;
    items?: OrderItemResponseDto[];
    createdAt?: string;
    updatedAt?: string;
}

export interface OrderSyncResultDto {
    orderId: number;
    orderCode?: string;
    syncStatus: OrderSyncStatus;
    nhanhSyncStage?: NhanhSyncStage;
    nhanhOrderId?: string;
    nhanhOrderCode?: string;
    syncError?: string;
    lastSyncMessage?: string;
    lastSyncAt?: string;
}

export interface CreateOrderPaymentDto {
    type: OrderPaymentType;
    paymentMethod: PaymentMethod;
}

export interface OrderPaymentResponseDto {
    id: number;
    orderId: number;
    paymentCode: string;
    type: OrderPaymentType;
    paymentMethod: PaymentMethod;
    status: PaymentStatus;
    amount: number;
    provider?: string | null;
    providerReference?: string | null;
    providerOrderCode?: number | null;
    checkoutUrl?: string | null;
    qrCode?: string | null;
    failureReason?: string | null;
    expiresAt?: string | null;
    paidAt?: string | null;
    createdAt: string;
    updatedAt: string;
}

export interface AdminOrderQueryParams {
    page?: number;
    size?: number;
    sortBy?: string;
    sortDirection?: 'ASC' | 'DESC';
}

export interface CustomerOrderListItemDto {
    id: number;
    orderCode?: string;
    type?: RequestType;
    status?: OrderStatus;
    totalAmount?: number;
    paidAmount?: number;
    remainingAmount?: number;
    paymentStatus?: PaymentStatus;
    createdAt?: string;
}

export interface CustomerOrderQueryParams {
    page?: number;
    size?: number;
    query?: string;
    status?: OrderStatus | 'ALL';
    createdFrom?: string;
    createdTo?: string;
    sortBy?: 'createdAt' | 'totalAmount' | 'status';
    sortDirection?: 'ASC' | 'DESC';
}
