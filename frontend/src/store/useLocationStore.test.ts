import { afterEach, beforeEach, describe, expect, it, jest } from '@jest/globals';
import { ApiResponseDTO } from '../interface/api-response';
import { ProvinceListResponse } from '../interface/location.model';
import { LocationService } from '../service/location.service';
import { useLocationStore } from './useLocationStore';

jest.mock('../service/location.service');

const mockedLocationService = jest.mocked(LocationService);

const provinceList: ProvinceListResponse = {
    datasetVersion: 'v2',
    provinces: [{
        id: 254,
        name: 'Ha Noi'
    }]
};

describe('useLocationStore', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        useLocationStore.getState().cancelScheduledRetry();
        useLocationStore.setState({
            provinces: [],
            wards: [],
            selectedProvinceId: null,
            datasetVersion: null,
            locationsLoaded: false,
            isLoading: false,
            isLoadingWards: false,
            message: null,
            error: null
        });
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    it('caches a successful provinces response', async () => {
        mockedLocationService.getProvinces.mockResolvedValue({
            success: true,
            statusCode: 200,
            message: 'Provinces retrieved successfully',
            data: provinceList
        });

        await useLocationStore.getState().fetchProvinces();
        await useLocationStore.getState().fetchProvinces();

        expect(mockedLocationService.getProvinces).toHaveBeenCalledTimes(1);
        expect(useLocationStore.getState().provinces).toEqual(provinceList.provinces);
        expect(useLocationStore.getState().datasetVersion).toBe('v2');
        expect(useLocationStore.getState().locationsLoaded).toBe(true);
    });

    it('deduplicates concurrent requests and keeps provinces loaded', async () => {
        let resolveRequest:
            ((response: ApiResponseDTO<ProvinceListResponse>) => void)
            | undefined;
        mockedLocationService.getProvinces.mockImplementation(() => (
            new Promise((resolve) => {
                resolveRequest = resolve;
            })
        ));

        const firstRequest = useLocationStore.getState().fetchProvinces();
        const secondRequest = useLocationStore.getState().fetchProvinces();
        resolveRequest?.({
            success: true,
            statusCode: 200,
            message: 'Provinces retrieved successfully',
            data: provinceList
        });
        await Promise.all([firstRequest, secondRequest]);

        expect(mockedLocationService.getProvinces).toHaveBeenCalledTimes(1);
        expect(useLocationStore.getState().locationsLoaded).toBe(true);
    });

    it('honors Retry-After and schedules only one cold-start retry', async () => {
        jest.useFakeTimers();
        mockedLocationService.getProvinces
            .mockRejectedValueOnce({
                response: {
                    status: 503,
                    headers: { 'retry-after': '2' },
                    data: { message: 'Location data is still being initialized' }
                }
            })
            .mockResolvedValueOnce({
                success: true,
                statusCode: 200,
                message: 'Provinces retrieved successfully',
                data: provinceList
            });

        await useLocationStore.getState().fetchProvinces(true);
        await useLocationStore.getState().fetchProvinces(true);
        expect(mockedLocationService.getProvinces).toHaveBeenCalledTimes(1);

        jest.advanceTimersByTime(2_000);
        expect(mockedLocationService.getProvinces).toHaveBeenCalledTimes(2);
        await mockedLocationService.getProvinces.mock.results[1].value;
        await Promise.resolve();
        expect(useLocationStore.getState().locationsLoaded).toBe(true);
    });

    it('loads wards for a selected province', async () => {
        mockedLocationService.getWards.mockResolvedValue({
            success: true,
            statusCode: 200,
            message: 'Wards retrieved successfully',
            data: {
                datasetVersion: 'v2',
                wards: [{ id: 1116, name: 'Phuc Xa' }]
            }
        });

        await useLocationStore.getState().fetchWards(254);

        expect(mockedLocationService.getWards).toHaveBeenCalledWith(254);
        expect(useLocationStore.getState().wards).toEqual([{ id: 1116, name: 'Phuc Xa' }]);
        expect(useLocationStore.getState().selectedProvinceId).toBe(254);
    });

    it('clears wards when province changes', async () => {
        await useLocationStore.getState().fetchWards(254);

        useLocationStore.getState().selectProvince(1);

        expect(useLocationStore.getState().wards).toEqual([]);
        expect(useLocationStore.getState().selectedProvinceId).toBe(1);
    });
});
