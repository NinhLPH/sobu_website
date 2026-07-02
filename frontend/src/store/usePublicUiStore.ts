import {create} from 'zustand';
import {BannerPosition, DeviceType} from '../enum/union-types';
import {BannerDTO, WebsiteConfigurationDTO} from '../interface/public-ui-config.model';
import {PublicUiService} from '../service/public-ui.service';
import {mapPublicConfigs, PublicConfigMap} from '../utils/website-config';

let bannersRequest: Promise<void> | null = null;
let configsRequest: Promise<void> | null = null;

const errorMessage = (error: any, fallback: string) =>
    error?.response?.data?.message || error?.message || fallback;

interface PublicUiState {
    banners: BannerDTO[];
    configs: WebsiteConfigurationDTO[];
    configMap: PublicConfigMap;
    bannersLoaded: boolean;
    configsLoaded: boolean;
    isBannersLoading: boolean;
    isConfigsLoading: boolean;
    bannersError: string | null;
    configsError: string | null;
    fetchBanners: (force?: boolean) => Promise<void>;
    fetchConfigs: (force?: boolean) => Promise<void>;
    invalidateBanners: () => void;
    invalidateConfigs: () => void;
}

export const usePublicUiStore = create<PublicUiState>((set, get) => ({
    banners: [],
    configs: [],
    configMap: {},
    bannersLoaded: false,
    configsLoaded: false,
    isBannersLoading: false,
    isConfigsLoading: false,
    bannersError: null,
    configsError: null,

    fetchBanners: async (force = false) => {
        if (!force && get().bannersLoaded) return;
        if (bannersRequest) return bannersRequest;

        bannersRequest = (async () => {
            set({isBannersLoading: true, bannersError: null});
            try {
                const banners = await PublicUiService.getBanners();
                set({banners: banners || [], bannersLoaded: true, isBannersLoading: false});
            } catch (error) {
                set({
                    bannersError: errorMessage(error, 'Không thể tải banner.'),
                    bannersLoaded: true,
                    isBannersLoading: false,
                });
            } finally {
                bannersRequest = null;
            }
        })();
        return bannersRequest;
    },

    fetchConfigs: async (force = false) => {
        if (!force && get().configsLoaded) return;
        if (configsRequest) return configsRequest;

        configsRequest = (async () => {
            set({isConfigsLoading: true, configsError: null});
            try {
                const configs = await PublicUiService.getConfigs();
                const publicConfigs = configs || [];
                set({
                    configs: publicConfigs,
                    configMap: mapPublicConfigs(publicConfigs),
                    configsLoaded: true,
                    isConfigsLoading: false,
                });
            } catch (error) {
                set({
                    configsError: errorMessage(error, 'Không thể tải cấu hình website.'),
                    configsLoaded: true,
                    isConfigsLoading: false,
                });
            } finally {
                configsRequest = null;
            }
        })();
        return configsRequest;
    },

    invalidateBanners: () => set({bannersLoaded: false}),
    invalidateConfigs: () => set({configsLoaded: false}),
}));

export const getBannersForPlacement = (
    banners: BannerDTO[],
    position: BannerPosition,
    deviceType: Exclude<DeviceType, 'ALL'>
) => banners
    .filter((banner) =>
        banner.position === position &&
        banner.isActive &&
        (banner.deviceType === deviceType || banner.deviceType === 'ALL')
    )
    .sort((left, right) => (left.displayOrder ?? Number.MAX_SAFE_INTEGER) - (right.displayOrder ?? Number.MAX_SAFE_INTEGER));

export const getPublicConfigValue = (
    configs: WebsiteConfigurationDTO[],
    key: string,
    fallback: string
) => configs.find((config) => config.key === key && config.isPublic)?.value?.trim() || fallback;

export const resetPublicUiRequestsForTests = () => {
    bannersRequest = null;
    configsRequest = null;
};
