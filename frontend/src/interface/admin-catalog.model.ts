import {PageResponse} from './api-response';

export interface AdminProductListItem {
    id: number;
    externalId?: number | string | null;
    name: string;
    slug?: string | null;
    code?: string | null;
    sku?: string | null;
    price?: number | null;
    retailPrice?: number | null;
    oldPrice?: number | null;
    salePrice?: number | null;
    status?: string | null;
    active?: boolean | null;
    avatarImage?: string | null;
    categoryId?: number | null;
    categoryName?: string | null;
    brandId?: number | null;
    brandName?: string | null;
    badgeId?: number | null;
    badgeName?: string | null;
    badgeColor?: string | null;
    badgeTextColor?: string | null;
    stockAvailable?: number | null;
    stockRemain?: number | null;
}

export interface AdminProductDetail extends AdminProductListItem {
    barcode?: string | null;
    otherName?: string | null;
    description?: string | null;
    content?: string | null;
    wholesalePrice?: number | null;
    importPrice?: number | null;
    vat?: number | null;
    images?: string[];
    length?: number | null;
    width?: number | null;
    height?: number | null;
    weight?: number | null;
}

export interface ProductWriteRequest {
    code: string;
    barcode?: string;
    name: string;
    otherName?: string;
    categoryId?: number | null;
    brandId?: number | null;
    badgeId?: number | null;
    retailPrice: number;
    importPrice?: number | null;
    wholesalePrice?: number | null;
    oldPrice?: number | null;
    vat?: number | null;
    avatarImage?: string;
    images?: string[];
    units?: Array<{ name: string; quantity: number; price: number; wholesalePrice?: number }>;
    attributes?: Array<{ name: string; value: string }>;
    description?: string;
    content?: string;
    length?: number | null;
    width?: number | null;
    height?: number | null;
    weight?: number | null;
    status?: string;
    active?: boolean;
}

export interface AdminCategory {
    id: number;
    code: string;
    name: string;
    parentId?: number | null;
    order?: number | null;
    image?: string | null;
    content?: string | null;
    status: number;
    children?: AdminCategory[];
}

export interface CategoryWriteRequest {
    code: string;
    name: string;
    parentId?: number | null;
    order?: number | null;
    image?: string;
    content?: string;
    status: number;
}

export interface AdminBrand {
    id: number;
    code: string;
    name: string;
    parentId?: number | null;
    status: number;
}

export interface BrandWriteRequest {
    code: string;
    name: string;
    parentId?: number | null;
    status: number;
}

export interface ProductBadge {
    id: number;
    name: string;
    color: string;
    textColor: string;
    status: number;
    createdAt?: string;
}

export interface ProductBadgeWriteRequest {
    name: string;
    color: string;
    textColor: string;
    status: number;
}

export type InventoryAdjustmentType =
    | 'OPENING_STOCK'
    | 'STOCK_IN'
    | 'STOCK_OUT'
    | 'CORRECTION'
    | 'DAMAGED'
    | 'RETURNED'
    | 'ORDER_RESERVATION'
    | 'ORDER_RELEASE';

export interface InventoryBalance {
    productId: number;
    stockRemain: number;
    stockAvailable: number;
    reserved: number;
}

export interface InventoryAdjustment {
    id: number;
    productId: number;
    type: InventoryAdjustmentType;
    quantityDelta: number;
    balanceAfter: number;
    orderId?: number | null;
    orderCode?: string | null;
    note?: string | null;
    actor?: string | null;
    createdAt?: string;
}

export type AdminProductPage = PageResponse<AdminProductListItem>;

export type VoucherType = 'DISCOUNT_PERCENT' | 'DISCOUNT_AMOUNT' | 'FREE_SHIP';
export type VoucherSlot = 'ITEM' | 'ORDER' | 'SHIPPING';
export type VoucherScope = 'ALL' | 'PRODUCT' | 'CATEGORY';
export type GeoScope = 'ALL' | 'HANOI_CENTER';

export interface Voucher {
    id: number;
    code: string;
    name: string;
    type: VoucherType;
    slot: VoucherSlot;
    scope: VoucherScope;
    geoScope: GeoScope;
    value: number;
    maxDiscountAmount?: number | null;
    minOrderValue?: number | null;
    usageLimit?: number | null;
    usedCount?: number | null;
    autoApply?: boolean;
    applicableProductIds?: number[];
    applicableCategoryIds?: number[];
    startDate?: string | null;
    endDate?: string | null;
    active?: boolean;
    deleted?: boolean;
    createdAt?: string;
}

export type VoucherWriteRequest = Omit<Voucher, 'id' | 'usedCount' | 'deleted' | 'createdAt'>;

export interface SpringPage<T> {
    content: T[];
    number: number;
    size: number;
    totalElements: number;
    totalPages: number;
    first?: boolean;
    last?: boolean;
}
