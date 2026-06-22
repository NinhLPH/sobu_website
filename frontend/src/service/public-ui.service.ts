import apiClient from "../api/api-client";
import {BannerDTO, WebsiteConfigurationDTO} from "../interface/public-ui-config.model";
import {BannerPosition, DeviceType} from '../enum/union-types';


export const PublicUiService = {
    getBanners: (filters?: {deviceType?: DeviceType; position?: BannerPosition}): Promise<BannerDTO[]> => {
        return apiClient.get('/api/public/ui/banners', {params: filters});
    },

    getConfigs: (): Promise<WebsiteConfigurationDTO[]> => {
        return apiClient.get('/api/public/ui/configs');
    },

    getConfigByKey: (key: string): Promise<WebsiteConfigurationDTO> => {
        return apiClient.get(`/api/public/ui/configs/${key}`);
    },
};
