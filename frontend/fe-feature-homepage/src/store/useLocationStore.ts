import { create } from 'zustand';
import { LocationTreeResponse } from '../interface/location.model';
import { LocationService } from '../service/location.service';

const getErrorMessage = (error: any, fallback: string) =>
    error?.response?.data?.message ||
    error?.response?.data?.error ||
    error?.message ||
    fallback;

let inFlightRequest: Promise<void> | null = null;

interface LocationState {
    locationTree: LocationTreeResponse | null;
    locationsLoaded: boolean;
    isLoading: boolean;
    message: string | null;
    error: string | null;
    fetchLocations: () => Promise<void>;
}

export const useLocationStore = create<LocationState>((set, get) => ({
    locationTree: null,
    locationsLoaded: false,
    isLoading: false,
    message: null,
    error: null,

    fetchLocations: async () => {
        if (get().locationsLoaded) {
            return;
        }

        if (inFlightRequest) {
            return inFlightRequest;
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
            } catch (error) {
                set({
                    error: getErrorMessage(
                        error,
                        'Không thể tải danh sách tỉnh, quận và phường.'
                    )
                });
            } finally {
                set({ isLoading: false });
                inFlightRequest = null;
            }
        })();

        return inFlightRequest;
    }
}));
