import { beforeEach, describe, expect, it, jest } from '@jest/globals';

const mockExternalGet = jest.fn<Promise<any>, any[]>();
const mockBackendGet = jest.fn<Promise<any>, any[]>();

jest.mock('axios', () => ({
    __esModule: true,
    default: {
        create: () => ({ get: (...args: any[]) => mockExternalGet(...args) })
    }
}));

jest.mock('../api/api-client', () => ({
    __esModule: true,
    default: {
        get: (...args: any[]) => mockBackendGet(...args)
    }
}));

const { LocationService } = require('./location.service');

describe('LocationService', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('maps post-2025 provinces from the external v2 API', async () => {
        mockExternalGet.mockResolvedValue({
            data: [
                { code: 79, name: 'Thành phố Hồ Chí Minh' },
                { code: 1, name: 'Thành phố Hà Nội' }
            ]
        });

        await expect(LocationService.getExternalProvinces()).resolves.toEqual({
            datasetVersion: 'api-v2',
            provinces: [
                { id: 1, name: 'Thành phố Hà Nội' },
                { id: 79, name: 'Thành phố Hồ Chí Minh' }
            ]
        });
        expect(mockExternalGet).toHaveBeenCalledWith('/p/', { signal: undefined });
    });

    it('maps wards and rejects a province mismatch', async () => {
        mockExternalGet.mockResolvedValueOnce({
            data: [{ code: 4, name: 'Phường Ba Đình', province_code: 1 }]
        });
        await expect(LocationService.getExternalWards(1)).resolves.toEqual({
            datasetVersion: 'api-v2',
            wards: [{ id: 4, name: 'Phường Ba Đình' }]
        });

        mockExternalGet.mockResolvedValueOnce({
            data: [{ code: 4, name: 'Phường Ba Đình', province_code: 79 }]
        });
        await expect(LocationService.getExternalWards(1)).rejects.toThrow(/outside the selected province/i);
    });

    it('keeps backend address calls on the SOBU API client', () => {
        LocationService.getBackendProvinces();
        LocationService.getBackendWards(1);

        expect(mockBackendGet).toHaveBeenNthCalledWith(1, '/api/public/address/provinces', { signal: undefined });
        expect(mockBackendGet).toHaveBeenNthCalledWith(2, '/api/public/address/wards', {
            params: { provinceId: 1 },
            signal: undefined
        });
    });
});
