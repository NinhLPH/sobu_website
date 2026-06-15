import apiClient from "../api/api-client";
import {BannerDTO, WebsiteConfigurationDTO} from "../interface/public-ui-config.model";


export const PublicUiService = {
    getBanners: (deviceType: 'WEB' | 'MOBILE' | 'ALL', position: 'HOME_TOP' | 'HOME_MIDDLE' | 'PRODUCT_SIDEBAR'): Promise<BannerDTO[]> => {
        return apiClient.get('/api/public/ui/banners', { params: { deviceType, position } });
    },

    getConfigs: (): Promise<WebsiteConfigurationDTO[]> => {
        return apiClient.get('/api/public/ui/configs');
    },

    getConfigByKey: (key: string): Promise<WebsiteConfigurationDTO> => {
        return apiClient.get(`/api/public/ui/configs/${key}`);
    },
};