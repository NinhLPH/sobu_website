import apiClient from '../api/api-client';
import {
    AdminBrand,
    AdminCategory,
    AdminProductDetail,
    AdminProductPage,
    BrandWriteRequest,
    CategoryWriteRequest,
    InventoryAdjustment,
    InventoryAdjustmentType,
    InventoryBalance,
    InventoryProductPage,
    ProductBadge,
    ProductBadgeWriteRequest,
    ProductWriteRequest,
    SpringPage,
    Voucher,
    VoucherScope,
    VoucherSlot,
    VoucherWriteRequest
} from '../interface/admin-catalog.model';

export type ProductParams = {
    page?: number;
    pageSize?: number;
    search?: string;
    categoryId?: number;
    brandId?: number;
    status?: string;
    active?: boolean;
    onSale?: boolean;
    inStock?: boolean;
    sortBy?: string;
    sortDirection?: 'ASC' | 'DESC';
};

export type InventoryProductParams = {
    search?: string;
    stockStatus?: 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK';
    page?: number;
    pageSize?: number;
    sortBy?: 'name' | 'stockAvailable';
    sortDirection?: 'ASC' | 'DESC';
};

type VoucherParams = {
    keyword?: string;
    active?: boolean;
    scope?: VoucherScope;
    slot?: VoucherSlot;
    autoApply?: boolean;
    page?: number;
    size?: number;
    sort?: string;
};

export const AdminCatalogService = {
    getProducts: (params?: ProductParams, signal?: AbortSignal): Promise<AdminProductPage> =>
        apiClient.get('/api/admin/products', { params, signal }),
    getInventoryProducts: (params?: InventoryProductParams, signal?: AbortSignal): Promise<InventoryProductPage> =>
        apiClient.get('/api/admin/inventory', {params, signal}),
    getProduct: (id: number): Promise<AdminProductDetail> =>
        apiClient.get(`/api/admin/products/${id}`),
    createProduct: (data: ProductWriteRequest): Promise<AdminProductDetail> =>
        apiClient.post('/api/admin/products', data),
    updateProduct: (id: number, data: ProductWriteRequest): Promise<AdminProductDetail> =>
        apiClient.put(`/api/admin/products/${id}`, data),
    setProductActive: (id: number, active: boolean, reason?: string): Promise<AdminProductDetail> =>
        apiClient.patch(`/api/admin/products/${id}/active`, { active, reason }),
    archiveProduct: (id: number, reason?: string): Promise<AdminProductDetail> =>
        apiClient.post(`/api/admin/products/${id}/archive`, { reason }),

    getCategories: (): Promise<AdminCategory[]> => apiClient.get('/api/admin/categories'),
    getCategory: (id: number): Promise<AdminCategory> => apiClient.get(`/api/admin/categories/${id}`),
    createCategory: (data: CategoryWriteRequest): Promise<AdminCategory> =>
        apiClient.post('/api/admin/categories', data),
    updateCategory: (id: number, data: CategoryWriteRequest): Promise<AdminCategory> =>
        apiClient.put(`/api/admin/categories/${id}`, data),
    deleteCategory: (id: number): Promise<void> => apiClient.delete(`/api/admin/categories/${id}`),
    setCategoryStatus: (id: number, status: number): Promise<AdminCategory> =>
        apiClient.patch(`/api/admin/categories/${id}/status`, { status }),

    getBrands: (): Promise<AdminBrand[]> => apiClient.get('/api/admin/brands'),
    getBrand: (id: number): Promise<AdminBrand> => apiClient.get(`/api/admin/brands/${id}`),
    createBrand: (data: BrandWriteRequest): Promise<AdminBrand> =>
        apiClient.post('/api/admin/brands', data),
    updateBrand: (id: number, data: BrandWriteRequest): Promise<AdminBrand> =>
        apiClient.put(`/api/admin/brands/${id}`, data),
    deleteBrand: (id: number): Promise<void> => apiClient.delete(`/api/admin/brands/${id}`),
    setBrandStatus: (id: number, status: number): Promise<AdminBrand> =>
        apiClient.patch(`/api/admin/brands/${id}/status`, { status }),

    getBadges: (): Promise<ProductBadge[]> => apiClient.get('/api/admin/badges'),
    getBadge: (id: number): Promise<ProductBadge> => apiClient.get(`/api/admin/badges/${id}`),
    createBadge: (data: ProductBadgeWriteRequest): Promise<ProductBadge> =>
        apiClient.post('/api/admin/badges', data),
    updateBadge: (id: number, data: ProductBadgeWriteRequest): Promise<ProductBadge> =>
        apiClient.put(`/api/admin/badges/${id}`, data),
    deleteBadge: (id: number): Promise<void> => apiClient.delete(`/api/admin/badges/${id}`),
    setBadgeStatus: (id: number, status: number): Promise<ProductBadge> =>
        apiClient.patch(`/api/admin/badges/${id}/status`, { status }),

    getInventoryBalance: (productId: number, signal?: AbortSignal): Promise<InventoryBalance> =>
        apiClient.get(`/api/admin/inventory/${productId}/balance`, { signal }),
    getInventoryLedger: (productId: number, signal?: AbortSignal): Promise<InventoryAdjustment[]> =>
        apiClient.get(`/api/admin/inventory/${productId}/ledger`, { signal }),
    setOpeningStock: (
        productId: number,
        quantity: number,
        note?: string,
        signal?: AbortSignal
    ): Promise<InventoryAdjustment> =>
        apiClient.post(`/api/admin/inventory/${productId}/opening`, { quantity, note }, { signal }),
    adjustInventory: (
        productId: number,
        type: Exclude<InventoryAdjustmentType, 'OPENING_STOCK' | 'ORDER_RESERVATION' | 'ORDER_RELEASE'>,
        quantity: number,
        note?: string,
        signal?: AbortSignal
    ): Promise<InventoryAdjustment> =>
        apiClient.post(`/api/admin/inventory/${productId}/adjustments`, { type, quantity, note }, { signal }),
};

export const AdminVoucherService = {
    getVouchers: (params?: VoucherParams): Promise<SpringPage<Voucher>> =>
        apiClient.get('/api/admin/vouchers', { params }),
    getAllVouchers: (): Promise<Voucher[]> => apiClient.get('/api/admin/vouchers/all'),
    createVoucher: (data: VoucherWriteRequest): Promise<Voucher> =>
        apiClient.post('/api/admin/vouchers', data),
    updateVoucher: (id: number, data: VoucherWriteRequest): Promise<Voucher> =>
        apiClient.put(`/api/admin/vouchers/${id}`, data),
    toggleVoucher: (id: number): Promise<Voucher> => apiClient.patch(`/api/admin/vouchers/${id}/toggle`),
    deleteVoucher: (id: number): Promise<void> => apiClient.delete(`/api/admin/vouchers/${id}`),
};
