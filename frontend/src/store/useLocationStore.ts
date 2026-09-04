import { create } from 'zustand';
import {
    LocationProvince,
    LocationSource,
    LocationWard,
    ProvinceListResponse,
    WardListResponse
} from '../interface/location.model';
import { LocationService } from '../service/location.service';

const CACHE_TTL_MS = 24 * 60 * 60 * 1000;
const CACHE_PREFIX = 'sobu:locations';

interface CacheEntry<T> {
    expiresAt: number;
    data: T;
}

const memoryCache = new Map<string, CacheEntry<unknown>>();
let provinceRequest: Promise<void> | null = null;
let wardRequestSequence = 0;
let wardAbortController: AbortController | null = null;
let retryTimer: ReturnType<typeof setTimeout> | null = null;
let retryAt = 0;

const cacheKey = (...parts: Array<string | number>) =>
    [CACHE_PREFIX, ...parts].join(':');

const readCache = <T>(key: string): T | null => {
    const memoryEntry = memoryCache.get(key) as CacheEntry<T> | undefined;
    if (memoryEntry && memoryEntry.expiresAt > Date.now()) return memoryEntry.data;
    if (memoryEntry) memoryCache.delete(key);
    try {
        const raw = window.sessionStorage.getItem(key);
        if (!raw) return null;
        const entry = JSON.parse(raw) as CacheEntry<T>;
        if (!entry || entry.expiresAt <= Date.now()) {
            window.sessionStorage.removeItem(key);
            return null;
        }
        memoryCache.set(key, entry);
        return entry.data;
    } catch {
        return null;
    }
};

const writeCache = <T>(key: string, data: T) => {
    const entry: CacheEntry<T> = { expiresAt: Date.now() + CACHE_TTL_MS, data };
    memoryCache.set(key, entry);
    try {
        window.sessionStorage.setItem(key, JSON.stringify(entry));
    } catch {
        // Memory cache remains available when browser storage is restricted.
    }
};

const getErrorMessage = (error: any, fallback: string) =>
    error?.response?.data?.message
    || error?.response?.data?.error
    || error?.message
    || fallback;

const retryDelayMs = (error: any): number | null => {
    if (![429, 503].includes(error?.response?.status)) return null;
    const raw = error?.response?.headers?.['retry-after'];
    const seconds = Number.parseInt(String(raw ?? ''), 10);
    if (Number.isFinite(seconds) && seconds > 0) return seconds * 1000;
    const date = Date.parse(String(raw ?? ''));
    if (Number.isFinite(date) && date > Date.now()) return date - Date.now();
    return 30_000;
};

const cancelRetryTimer = () => {
    if (retryTimer) clearTimeout(retryTimer);
    retryTimer = null;
};

const unwrap = <T>(response: { success: boolean; data: T; message?: string }, fallback: string): T => {
    if (!response?.success || !response.data) {
        throw new Error(response?.message || fallback);
    }
    return response.data;
};

interface InitializeOptions {
    force?: boolean;
    autoRetry?: boolean;
}

interface LocationState {
    provinces: LocationProvince[];
    wards: LocationWard[];
    selectedProvinceId: number | null;
    datasetVersion: string | null;
    source: LocationSource | null;
    nhanhEnabled: boolean | null;
    locationsLoaded: boolean;
    isLoading: boolean;
    isLoadingWards: boolean;
    message: string | null;
    notice: string | null;
    error: string | null;
    initialize: (nhanhEnabled: boolean, options?: InitializeOptions) => Promise<void>;
    fetchWards: (provinceId: number, force?: boolean) => Promise<void>;
    selectProvince: (provinceId: number | null) => void;
    retry: () => Promise<void>;
    cancelScheduledRetry: () => void;
}

export const useLocationStore = create<LocationState>((set, get) => ({
    provinces: [],
    wards: [],
    selectedProvinceId: null,
    datasetVersion: null,
    source: null,
    nhanhEnabled: null,
    locationsLoaded: false,
    isLoading: false,
    isLoadingWards: false,
    message: null,
    notice: null,
    error: null,

    cancelScheduledRetry: () => {
        cancelRetryTimer();
        retryAt = 0;
    },

    initialize: async (nhanhEnabled, options = {}) => {
        const { force = false, autoRetry = false } = options;
        const state = get();
        if (!force && state.locationsLoaded && state.nhanhEnabled === nhanhEnabled) return;
        if (provinceRequest) return provinceRequest;
        if (!force && Date.now() < retryAt) return;

        const preferredSource: LocationSource = nhanhEnabled ? 'backend-address' : 'external-v2';
        const cached = !force
            ? readCache<ProvinceListResponse>(cacheKey(preferredSource, 'provinces'))
            : null;
        if (cached?.provinces?.length) {
            set({
                provinces: cached.provinces,
                wards: [],
                selectedProvinceId: null,
                datasetVersion: cached.datasetVersion,
                source: preferredSource,
                nhanhEnabled,
                locationsLoaded: true,
                error: null,
                notice: null
            });
            return;
        }

        set({ isLoading: true, error: null, notice: null, nhanhEnabled });
        provinceRequest = (async () => {
            try {
                let data: ProvinceListResponse;
                let source: LocationSource = preferredSource;
                let notice: string | null = null;
                try {
                    data = nhanhEnabled
                        ? unwrap(await LocationService.getBackendProvinces(), 'Could not load provinces.')
                        : await LocationService.getExternalProvinces();
                } catch (externalError) {
                    if (nhanhEnabled) throw externalError;
                    data = unwrap(
                        await LocationService.getBackendProvinces(),
                        'Could not load fallback provinces.'
                    );
                    source = 'backend-fallback';
                    notice = 'API địa chỉ bên ngoài đang gián đoạn. Đang sử dụng dữ liệu dự phòng của SOBU.';
                }
                if (!data.provinces?.length) throw new Error('Province list is empty.');
                writeCache(cacheKey(source, 'provinces'), data);
                set({
                    provinces: data.provinces,
                    wards: [],
                    selectedProvinceId: null,
                    datasetVersion: data.datasetVersion,
                    source,
                    locationsLoaded: true,
                    message: 'Đã tải danh sách tỉnh, thành phố.',
                    notice,
                    error: null
                });
                cancelRetryTimer();
                retryAt = 0;
            } catch (error) {
                const delay = retryDelayMs(error);
                set({
                    locationsLoaded: false,
                    error: getErrorMessage(error, 'Không thể tải danh sách tỉnh, thành phố.')
                });
                if (delay !== null) {
                    retryAt = Date.now() + delay;
                    if (autoRetry && !retryTimer) {
                        retryTimer = setTimeout(() => {
                            retryTimer = null;
                            retryAt = 0;
                            void get().initialize(nhanhEnabled, { force: true, autoRetry: true });
                        }, delay);
                    }
                }
            } finally {
                set({ isLoading: false });
                provinceRequest = null;
            }
        })();
        return provinceRequest;
    },

    fetchWards: async (provinceId, force = false) => {
        if (!Number.isInteger(provinceId) || provinceId <= 0) {
            set({ wards: [], selectedProvinceId: null });
            return;
        }
        const state = get();
        if (!force && state.selectedProvinceId === provinceId && state.wards.length > 0) return;

        const sequence = ++wardRequestSequence;
        wardAbortController?.abort();
        wardAbortController = new AbortController();
        const source = state.source || 'backend-address';
        const version = state.datasetVersion || 'current';
        const cached = !force
            ? readCache<WardListResponse>(cacheKey(source, version, 'wards', provinceId))
            : null;
        if (cached?.wards?.length) {
            set({
                wards: cached.wards,
                selectedProvinceId: provinceId,
                datasetVersion: cached.datasetVersion,
                isLoadingWards: false,
                error: null
            });
            return;
        }

        set({ wards: [], selectedProvinceId: provinceId, isLoadingWards: true, error: null });
        try {
            let data: WardListResponse;
            let resolvedSource = source;
            let notice = state.notice;
            try {
                data = source === 'external-v2'
                    ? await LocationService.getExternalWards(provinceId, wardAbortController.signal)
                    : unwrap(
                        await LocationService.getBackendWards(provinceId, wardAbortController.signal),
                        'Could not load wards.'
                    );
            } catch (externalError: any) {
                if (externalError?.code === 'ERR_CANCELED' || sequence !== wardRequestSequence) return;
                if (source !== 'external-v2') throw externalError;
                data = unwrap(
                    await LocationService.getBackendWards(provinceId, wardAbortController.signal),
                    'Could not load fallback wards.'
                );
                resolvedSource = 'backend-fallback';
                notice = 'API địa chỉ bên ngoài đang gián đoạn. Đang sử dụng dữ liệu dự phòng của SOBU.';
            }
            if (sequence !== wardRequestSequence) return;
            if (!data.wards?.length) throw new Error('Ward list is empty.');
            writeCache(cacheKey(resolvedSource, data.datasetVersion, 'wards', provinceId), data);
            set({
                wards: data.wards,
                datasetVersion: data.datasetVersion,
                source: resolvedSource,
                notice,
                error: null
            });
        } catch (error: any) {
            if (error?.code !== 'ERR_CANCELED' && sequence === wardRequestSequence) {
                set({ error: getErrorMessage(error, 'Không thể tải danh sách phường, xã.') });
            }
        } finally {
            if (sequence === wardRequestSequence) set({ isLoadingWards: false });
        }
    },

    selectProvince: (provinceId) => {
        if (provinceId !== get().selectedProvinceId) {
            ++wardRequestSequence;
            wardAbortController?.abort();
            set({ wards: [], selectedProvinceId: provinceId, error: null });
            if (provinceId) void get().fetchWards(provinceId);
        }
    },

    retry: async () => {
        retryAt = 0;
        cancelRetryTimer();
        const state = get();
        if (state.selectedProvinceId && state.locationsLoaded) {
            await get().fetchWards(state.selectedProvinceId, true);
            return;
        }
        await get().initialize(Boolean(state.nhanhEnabled), { force: true, autoRetry: true });
    }
}));

export const clearLocationCacheForTests = () => {
    memoryCache.clear();
    provinceRequest = null;
    wardRequestSequence += 1;
    wardAbortController?.abort();
    wardAbortController = null;
    cancelRetryTimer();
    retryAt = 0;
};
