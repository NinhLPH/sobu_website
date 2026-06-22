import { afterEach, beforeEach, describe, expect, it, jest } from '@jest/globals';
import { ApiResponseDTO } from '../interface/api-response';
import { LocationTreeResponse } from '../interface/location.model';
import { LocationService } from '../service/location.service';
import { useLocationStore } from './useLocationStore';

jest.mock('../service/location.service');

const mockedLocationService = jest.mocked(LocationService);

const locationTree: LocationTreeResponse = {
    provider: 'NHANH',
    locationVersion: 'v1',
    cachedAt: '2026-06-13T03:00:00Z',
    expiresAt: '2026-06-14T03:00:00Z',
    stale: false,
    cities: [{
        cityId: 254,
        cityName: 'Ha Noi',
        otherName: null,
        districts: [{
            districtId: 331,
            districtName: 'Ba Dinh',
            otherName: null,
            wards: [{
                wardId: 1116,
                wardName: 'Phuc Xa',
                otherName: null
            }]
        }]
    }]
};

describe('useLocationStore', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        useLocationStore.getState().cancelScheduledRetry();
        useLocationStore.setState({
            locationTree: null,
            locationsLoaded: false,
            isLoading: false,
            message: null,
            error: null
        });
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    it('caches a successful location response', async () => {
        mockedLocationService.getLocations.mockResolvedValue({
            success: true,
            statusCode: 200,
            message: 'Locations retrieved successfully',
            data: locationTree
        });

        await useLocationStore.getState().fetchLocations();
        await useLocationStore.getState().fetchLocations();

        expect(mockedLocationService.getLocations).toHaveBeenCalledTimes(1);
        expect(useLocationStore.getState().locationTree).toEqual(locationTree);
        expect(useLocationStore.getState().locationsLoaded).toBe(true);
    });

    it('deduplicates concurrent requests and accepts stale cached data', async () => {
        let resolveRequest:
            ((response: ApiResponseDTO<LocationTreeResponse>) => void)
            | undefined;
        mockedLocationService.getLocations.mockImplementation(() => (
            new Promise((resolve) => {
                resolveRequest = resolve;
            })
        ));

        const firstRequest = useLocationStore.getState().fetchLocations();
        const secondRequest = useLocationStore.getState().fetchLocations();
        resolveRequest?.({
            success: true,
            statusCode: 200,
            message: 'Locations retrieved from stale cache',
            data: { ...locationTree, stale: true }
        });
        await Promise.all([firstRequest, secondRequest]);

        expect(mockedLocationService.getLocations).toHaveBeenCalledTimes(1);
        expect(useLocationStore.getState().locationTree?.stale).toBe(true);
        expect(useLocationStore.getState().message)
            .toBe('Locations retrieved from stale cache');
    });

    it('honors Retry-After and schedules only one cold-start retry', async () => {
        jest.useFakeTimers();
        mockedLocationService.getLocations
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
                message: 'Locations retrieved successfully',
                data: locationTree
            });

        await useLocationStore.getState().fetchLocations(true);
        await useLocationStore.getState().fetchLocations(true);
        expect(mockedLocationService.getLocations).toHaveBeenCalledTimes(1);

        jest.advanceTimersByTime(2_000);
        expect(mockedLocationService.getLocations).toHaveBeenCalledTimes(2);
        await mockedLocationService.getLocations.mock.results[1].value;
        await Promise.resolve();
        expect(useLocationStore.getState().locationsLoaded).toBe(true);
    });
});
