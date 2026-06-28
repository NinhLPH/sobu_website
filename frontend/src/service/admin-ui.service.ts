import apiClient from "../api/api-client";
import {PageResponse} from "../interface/api-response";
import {
    BannerDTO,
    BannerMutationPayload,
    UiSearchParams,
    WebsiteConfigurationDTO,
    WebsiteConfigurationMutationPayload
} from "../interface/public-ui-config.model";

export const buildBannerFormData = (payload: BannerMutationPayload, image?: File | null) => {
    const formData = new FormData();
    formData.append('banner', new Blob([JSON.stringify(payload)], {type: 'application/json'}));
    if (image) formData.append('image', image);
    return formData;
};

export const AdminUiService = {
    // --- BANNERS ---
    searchBanners: (searchTerm: string, params?: UiSearchParams): Promise<PageResponse<BannerDTO>> => {
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

    deleteBanner: (id: string | number): Promise<void> => {
        return apiClient.delete(`/api/admin/banners/${id}`);
    },

    // --- CONFIGURATIONS ---
    searchConfigs: (searchTerm: string, params?: UiSearchParams): Promise<PageResponse<WebsiteConfigurationDTO>> => {
        return apiClient.post('/api/admin/configs/search', { searchTerm, ...params });
    },

    getConfigDetail: (id: string | number): Promise<WebsiteConfigurationDTO> => {
        return apiClient.get(`/api/admin/configs/${id}`);
    },

    createConfig: (data: WebsiteConfigurationMutationPayload): Promise<WebsiteConfigurationDTO> => {
        return apiClient.post('/api/admin/configs', data);
    },

    updateConfig: (id: string | number, data: WebsiteConfigurationMutationPayload): Promise<WebsiteConfigurationDTO> => {
        return apiClient.put(`/api/admin/configs/${id}`, data);
    },

    bulkUpdateConfigs: (items: Array<{key: string; value: string}>): Promise<WebsiteConfigurationDTO[]> => {
        return apiClient.put('/api/admin/configs/bulk', items);
    },

    deleteConfig: (id: string | number): Promise<void> => {
        return apiClient.delete(`/api/admin/configs/${id}`);
    },
};
