export type VoucherSlot = 'ITEM' | 'ORDER' | 'SHIPPING';
export type VoucherType = 'DISCOUNT_PERCENT' | 'DISCOUNT_AMOUNT' | 'FREE_SHIP';
export type VoucherScope = 'ALL' | 'PRODUCT' | 'CATEGORY';
export type VoucherGeoScope = 'ALL' | 'HANOI_CENTER';

export interface ActiveVoucher {
    id: number;
    code: string;
    name: string;
    type: VoucherType;
    slot: VoucherSlot;
    scope: VoucherScope;
    value?: number | null;
    maxDiscountAmount?: number | null;
    minOrderValue?: number | null;
    autoApply?: boolean | null;
    endDate?: string | null;
}

export interface VoucherCartItem {
    productId: number;
    categoryId?: number;
    name: string;
    price: number;
    quantity: number;
}

export interface VoucherApplyRequest {
    discountVoucherCode?: string;
    shippingVoucherCode?: string;
    subtotal: number;
    shippingFee: number;
    items: VoucherCartItem[];
    customerCityName?: string;
    customerWardName?: string;
    customerCityId?: number;
    customerWardId?: number;
    autoApply: boolean;
}

export interface AppliedVoucher {
    voucherId: number;
    code: string;
    name: string;
    slot: VoucherSlot;
    type: VoucherType;
    discountAmount: number;
    autoApplied?: boolean;
}

export interface ProductVoucherSummary {
    id: number;
    code: string;
    name: string;
    type: VoucherType;
    slot: VoucherSlot;
    scope: VoucherScope;
    geoScope?: VoucherGeoScope | null;
    value?: number | null;
    maxDiscountAmount?: number | null;
    minOrderValue?: number | null;
    autoApply?: boolean | null;
    estimatedDiscount?: number | null;
    effectivePrice?: number | null;
    badgeText?: string | null;
    startDate?: string | null;
    endDate?: string | null;
}

export interface ProductVoucherQuery {
    categoryId?: number;
    oldPrice?: number;
    price?: number;
}

export interface VoucherApplyResponse {
    valid: boolean;
    discountVoucherCode?: string | null;
    discountVoucherName?: string | null;
    itemVoucherCode?: string | null;
    itemVoucherName?: string | null;
    orderVoucherCode?: string | null;
    orderVoucherName?: string | null;
    shippingVoucherCode?: string | null;
    shippingVoucherName?: string | null;
    itemDiscount: number;
    orderDiscount: number;
    subtotalDiscount: number;
    shippingDiscount: number;
    totalDiscount: number;
    originalSubtotal: number;
    originalShippingFee: number;
    finalSubtotal: number;
    finalShippingFee: number;
    finalTotal: number;
    appliedVouchers: AppliedVoucher[];
    message?: string | null;
}
