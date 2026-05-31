import apiClient from "../api/api-client";

export const AdminSyncService = {
    getNhanhLoginUrl: (): Promise<string> => {
        return apiClient.get('/api/nhanh/login');
    },

    handleNhanhCallback: (accessCode: string): Promise<string> => {
        return apiClient.get('/api/nhanh/oauth/callback', {params: {accessCode}});
    },

    syncProducts: (): Promise<{ message: string }> => {
        return apiClient.post('/api/admin/products/sync');
    },

    syncCategories: (): Promise<{ message: string }> => {
        return apiClient.post('/api/admin/categories/sync');
    },

    syncBrands: (): Promise<{ message: string }> => {
        return apiClient.post('/api/admin/brands/sync');
    },
};