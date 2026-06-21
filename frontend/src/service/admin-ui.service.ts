import apiClient from "../api/api-client";
import {PageResponse} from "../interface/api-response";
import {BannerDTO, WebsiteConfigurationDTO} from "../interface/public-ui-config.model";

export const AdminUiService = {
    // --- BANNERS ---
    searchBanners: (searchTerm: string, params?: any): Promise<PageResponse<BannerDTO>> => {
        return apiClient.post('/api/admin/banners/search', { searchTerm, ...params });
    },

    getBannerDetail: (id: string | number): Promise<BannerDTO> => {
        return apiClient.get(`/api/admin/banners/${id}`);
    },

    // Do yêu cầu multipart/form-data, bạn cần truyền formData chuẩn từ UI xuống
    createBanner: (formData: FormData): Promise<BannerDTO> => {
        return apiClient.post('/api/admin/banners', formData, {
            headers: { 'Content-Type': 'multipart/form-data' },
        });
    },

    updateBanner: (id: string | number, formData: FormData): Promise<BannerDTO> => {
        return apiClient.put(`/api/admin/banners/${id}`, formData, {
            headers: { 'Content-Type': 'multipart/form-data' },
        });
    },

    deleteBanner: (id: string | number): Promise<any> => {
        return apiClient.delete(`/api/admin/banners/${id}`);
    },

    // --- CONFIGURATIONS ---
    searchConfigs: (searchTerm: string, params?: any): Promise<PageResponse<WebsiteConfigurationDTO>> => {
        return apiClient.post('/api/admin/configs/search', { searchTerm, ...params });
    },

    getConfigDetail: (id: string | number): Promise<WebsiteConfigurationDTO> => {
        return apiClient.get(`/api/admin/configs/${id}`);
    },

    createConfig: (data: any): Promise<WebsiteConfigurationDTO> => {
        return apiClient.post('/api/admin/configs', data);
    },

    updateConfig: (id: string | number, data: any): Promise<WebsiteConfigurationDTO> => {
        return apiClient.put(`/api/admin/configs/${id}`, data);
    },

    deleteConfig: (id: string | number): Promise<any> => {
        return apiClient.delete(`/api/admin/configs/${id}`);
    },
};