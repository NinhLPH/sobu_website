import { create } from 'zustand';
import { LocationTreeResponse } from '../interface/location.model';
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
    locationTree: LocationTreeResponse | null;
    locationsLoaded: boolean;
    isLoading: boolean;
    message: string | null;
    error: string | null;
    fetchLocations: (autoRetry?: boolean) => Promise<void>;
    cancelScheduledRetry: () => void;
}

export const useLocationStore = create<LocationState>((set, get) => ({
    locationTree: null,
    locationsLoaded: false,
    isLoading: false,
    message: null,
    error: null,

    cancelScheduledRetry: () => {
        cancelRetryTimer();
        nextRetryAt = 0;
    },

    fetchLocations: async (autoRetry = false) => {
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
                const response = await LocationService.getLocations();
                if (!response.success || !response.data) {
                    throw new Error(response.message || 'Could not load shipping locations.');
                }

                set({
                    locationTree: response.data,
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
                        'Không thể tải danh sách tỉnh, quận và phường.'
                    )
                });
                if (delayMs !== null) {
                    nextRetryAt = Date.now() + delayMs;
                    if (autoRetry && !retryTimer) {
                        retryTimer = setTimeout(() => {
                            retryTimer = null;
                            nextRetryAt = 0;
                            void get().fetchLocations(true);
                        }, delayMs);
                    }
                }
            } finally {
                set({ isLoading: false });
                inFlightRequest = null;
            }
        })();

        return inFlightRequest;
    }
}));
