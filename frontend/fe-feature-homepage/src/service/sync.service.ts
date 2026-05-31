import apiClient from "../api/api-client";

export const AdminSyncService = {
    syncProducts: (): Promise<{ message: string }> => {
        return apiClient.post('/api/admin/products/sync');
    },

    syncCategories: (): Promise<{ message: string }> => {
        return apiClient.post('/api/admin/categories/sync');
    },

    // Lưu ý theo API Doc: Endpoint này là ngoại lệ, KHÔNG có prefix /api
    syncBrands: (): Promise<{ message: string }> => {
        return apiClient.post('/admin/brands/sync');
    },
};