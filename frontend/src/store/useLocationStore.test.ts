import { afterEach, beforeEach, describe, expect, it, jest } from '@jest/globals';
import { ApiResponseDTO } from '../interface/api-response';
import { ProvinceListResponse } from '../interface/location.model';
import { LocationService } from '../service/location.service';
import { clearLocationCacheForTests, useLocationStore } from './useLocationStore';

jest.mock('../service/location.service');

const mockedLocationService = jest.mocked(LocationService);
const provinceList: ProvinceListResponse = {
    datasetVersion: 'api-v2',
    provinces: [{ id: 1, name: 'Thành phố Hà Nội' }]
};
const backendProvinceResponse: ApiResponseDTO<ProvinceListResponse> = {
    success: true,
    statusCode: 200,
    message: 'Provinces retrieved',
    data: { ...provinceList, datasetVersion: '2025.1' }
};

describe('useLocationStore', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        window.sessionStorage.clear();
        clearLocationCacheForTests();
        useLocationStore.setState({
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
            error: null
        });
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    it('uses and caches the external v2 source in local mode', async () => {
        mockedLocationService.getExternalProvinces.mockResolvedValue(provinceList);

        await useLocationStore.getState().initialize(false);
        await useLocationStore.getState().initialize(false);

        expect(mockedLocationService.getExternalProvinces).toHaveBeenCalledTimes(1);
        expect(mockedLocationService.getBackendProvinces).not.toHaveBeenCalled();
        expect(useLocationStore.getState()).toEqual(expect.objectContaining({
            provinces: provinceList.provinces,
            datasetVersion: 'api-v2',
            source: 'external-v2',
            locationsLoaded: true
        }));
    });

    it('falls back to the backend address dataset when the external API fails', async () => {
        mockedLocationService.getExternalProvinces.mockRejectedValue(new Error('CORS blocked'));
        mockedLocationService.getBackendProvinces.mockResolvedValue(backendProvinceResponse);

        await useLocationStore.getState().initialize(false);

        expect(useLocationStore.getState().source).toBe('backend-fallback');
        expect(useLocationStore.getState().notice).toMatch(/dữ liệu dự phòng/i);
        expect(useLocationStore.getState().locationsLoaded).toBe(true);
    });

    it('uses only the backend address API when Nhanh is enabled', async () => {
        mockedLocationService.getBackendProvinces.mockResolvedValue(backendProvinceResponse);

        await useLocationStore.getState().initialize(true);

        expect(mockedLocationService.getExternalProvinces).not.toHaveBeenCalled();
        expect(mockedLocationService.getBackendProvinces).toHaveBeenCalledTimes(1);
        expect(useLocationStore.getState().source).toBe('backend-address');
    });

    it('deduplicates concurrent province requests', async () => {
        let resolveRequest: ((value: ProvinceListResponse) => void) | undefined;
        mockedLocationService.getExternalProvinces.mockImplementation(() => new Promise((resolve) => {
            resolveRequest = resolve;
        }));

        const first = useLocationStore.getState().initialize(false);
        const second = useLocationStore.getState().initialize(false);
        resolveRequest?.(provinceList);
        await Promise.all([first, second]);

        expect(mockedLocationService.getExternalProvinces).toHaveBeenCalledTimes(1);
    });

    it('falls back to backend wards and keeps the selected province', async () => {
        useLocationStore.setState({
            provinces: provinceList.provinces,
            locationsLoaded: true,
            source: 'external-v2',
            datasetVersion: 'api-v2',
            nhanhEnabled: false
        });
        mockedLocationService.getExternalWards.mockRejectedValue(new Error('network'));
        mockedLocationService.getBackendWards.mockResolvedValue({
            success: true,
            message: 'Wards retrieved',
            data: {
                datasetVersion: '2025.1',
                wards: [{ id: 4, name: 'Phường Ba Đình' }]
            }
        });

        await useLocationStore.getState().fetchWards(1);

        expect(useLocationStore.getState()).toEqual(expect.objectContaining({
            selectedProvinceId: 1,
            source: 'backend-fallback',
            wards: [{ id: 4, name: 'Phường Ba Đình' }]
        }));
    });

    it('ignores a stale ward response after the province changes', async () => {
        useLocationStore.setState({
            provinces: [
                { id: 1, name: 'Thành phố Hà Nội' },
                { id: 79, name: 'Thành phố Hồ Chí Minh' }
            ],
            locationsLoaded: true,
            source: 'external-v2',
            datasetVersion: 'api-v2',
            nhanhEnabled: false
        });
        let resolveFirst: ((value: { datasetVersion: string; wards: Array<{ id: number; name: string }> }) => void) | undefined;
        mockedLocationService.getExternalWards
            .mockImplementationOnce(() => new Promise((resolve) => {
                resolveFirst = resolve;
            }))
            .mockResolvedValueOnce({
                datasetVersion: 'api-v2',
                wards: [{ id: 26740, name: 'Phường Sài Gòn' }]
            });

        const first = useLocationStore.getState().fetchWards(1);
        const second = useLocationStore.getState().fetchWards(79);
        await second;
        resolveFirst?.({
            datasetVersion: 'api-v2',
            wards: [{ id: 4, name: 'Phường Ba Đình' }]
        });
        await first;

        expect(useLocationStore.getState()).toEqual(expect.objectContaining({
            selectedProvinceId: 79,
            wards: [{ id: 26740, name: 'Phường Sài Gòn' }]
        }));
    });

    it('honors Retry-After after both local-mode sources fail', async () => {
        jest.useFakeTimers();
        mockedLocationService.getExternalProvinces.mockRejectedValue(new Error('network'));
        mockedLocationService.getBackendProvinces
            .mockRejectedValueOnce({
                response: {
                    status: 503,
                    headers: { 'retry-after': '2' },
                    data: { message: 'Location data is initializing' }
                }
            })
            .mockResolvedValueOnce(backendProvinceResponse);

        await useLocationStore.getState().initialize(false, { autoRetry: true });
        jest.advanceTimersByTime(2_000);
        jest.runAllTicks();
        for (let index = 0; index < 6; index += 1) await Promise.resolve();

        expect(mockedLocationService.getBackendProvinces).toHaveBeenCalledTimes(2);
    });
});
