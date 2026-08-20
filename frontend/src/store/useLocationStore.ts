import { create } from 'zustand';
import { LocationProvince, LocationWard } from '../interface/location.model';
import { LocationService } from '../service/location.service';

const getErrorMessage = (error: any, fallback: string) =>
    error?.response?.data?.message ||
    error?.response?.data?.error ||
    error?.message ||
    fallback;

let inFlightRequest: Promise<void> | null = null;
let retryTimer: ReturnType<typeof setTimeout> | null = null;
let nextRetryAt = 0;

const cancelRetryTimer = () => {
    if (retryTimer) {
        clearTimeout(retryTimer);
        retryTimer = null;
    }
};

const retryDelayMs = (error: any): number | null => {
    if (error?.response?.status !== 503) {
        return null;
    }
    const rawHeader = error?.response?.headers?.['retry-after'];
    const seconds = Number.parseInt(String(rawHeader ?? ''), 10);
    return Number.isFinite(seconds) && seconds > 0 ? seconds * 1000 : 30_000;
};

interface LocationState {
    provinces: LocationProvince[];
    wards: LocationWard[];
    selectedProvinceId: number | null;
    datasetVersion: string | null;
    locationsLoaded: boolean;
    isLoading: boolean;
    isLoadingWards: boolean;
    message: string | null;
    error: string | null;
    fetchProvinces: (autoRetry?: boolean) => Promise<void>;
    fetchWards: (provinceId: number) => Promise<void>;
    selectProvince: (provinceId: number | null) => void;
    cancelScheduledRetry: () => void;
}

export const useLocationStore = create<LocationState>((set, get) => ({
    provinces: [],
    wards: [],
    selectedProvinceId: null,
    datasetVersion: null,
    locationsLoaded: false,
    isLoading: false,
    isLoadingWards: false,
    message: null,
    error: null,

    cancelScheduledRetry: () => {
        cancelRetryTimer();
        nextRetryAt = 0;
    },

    fetchProvinces: async (autoRetry = false) => {
        if (get().locationsLoaded) {
            return;
        }
        if (inFlightRequest) {
            return inFlightRequest;
        }
        if (Date.now() < nextRetryAt) {
            return;
        }

        set({ isLoading: true, error: null });
        inFlightRequest = (async () => {
            try {
                const response = await LocationService.getProvinces();
                if (!response.success || !response.data) {
                    throw new Error(response.message || 'Could not load provinces.');
                }

                set({
                    provinces: response.data.provinces ?? [],
                    datasetVersion: response.data.datasetVersion ?? null,
                    locationsLoaded: true,
                    message: response.message,
                    error: null
                });
                cancelRetryTimer();
                nextRetryAt = 0;
            } catch (error) {
                const delayMs = retryDelayMs(error);
                set({
                    error: getErrorMessage(
                        error,
                        'Không thể tải danh sách tỉnh, thành phố.'
                    )
                });
                if (delayMs !== null) {
                    nextRetryAt = Date.now() + delayMs;
                    if (autoRetry && !retryTimer) {
                        retryTimer = setTimeout(() => {
                            retryTimer = null;
                            nextRetryAt = 0;
                            void get().fetchProvinces(true);
                        }, delayMs);
                    }
                }
            } finally {
                set({ isLoading: false });
                inFlightRequest = null;
            }
        })();

        return inFlightRequest;
    },

    fetchWards: async (provinceId: number) => {
        if (!provinceId) {
            set({ wards: [], selectedProvinceId: null });
            return;
        }
        if (get().selectedProvinceId === provinceId && get().wards.length > 0) {
            return;
        }

        set({ isLoadingWards: true, selectedProvinceId: provinceId });
        try {
            const response = await LocationService.getWards(provinceId);
            if (!response.success || !response.data) {
                throw new Error(response.message || 'Could not load wards.');
            }

            set({
                wards: response.data.wards ?? [],
                datasetVersion: response.data.datasetVersion ?? get().datasetVersion
            });
        } catch (error) {
            set({ error: getErrorMessage(error, 'Không thể tải danh sách phường, xã.') });
        } finally {
            set({ isLoadingWards: false });
        }
    },

    selectProvince: (provinceId) => {
        if (provinceId !== get().selectedProvinceId) {
            set({ wards: [], selectedProvinceId: provinceId });
            if (provinceId) {
                void get().fetchWards(provinceId);
            }
        }
    }
}));
