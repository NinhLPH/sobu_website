import {beforeEach, describe, expect, it, jest} from '@jest/globals';
import apiClient from '../api/api-client';
import {AdminCatalogService} from './admin-catalog.service';
import {
    DEFAULT_LOW_STOCK_THRESHOLD,
    InventoryDashboardService,
    inventoryQuantity,
    parseLowStockThreshold
} from './inventory-dashboard.service';

jest.mock('./admin-catalog.service');
jest.mock('../api/api-client');

const mockedCatalog = jest.mocked(AdminCatalogService);

describe('InventoryDashboardService', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('reads and validates the configured low-stock threshold', async () => {
        (apiClient.get as any).mockResolvedValue({value: '7'});
        await expect(InventoryDashboardService.getLowStockThreshold()).resolves.toBe(7);
        expect(apiClient.get).toHaveBeenCalledWith(
            '/api/public/configs/key/business_low_stock_threshold',
            {signal: undefined}
        );
        expect(parseLowStockThreshold('-1')).toBe(DEFAULT_LOW_STOCK_THRESHOLD);
        expect(parseLowStockThreshold('invalid')).toBe(DEFAULT_LOW_STOCK_THRESHOLD);
    });

    it('falls back to five when the configuration API fails', async () => {
        (apiClient.get as any).mockRejectedValue(new Error('Config unavailable'));
        await expect(InventoryDashboardService.getLowStockThreshold()).resolves.toBe(5);
    });

    it('paginates ascending stock and stops after the first product above threshold', async () => {
        mockedCatalog.getProducts
            .mockResolvedValueOnce({content: [
                {id: 1, name: 'Hết hàng', stockAvailable: 0},
                {id: 2, name: 'Còn ít', stockAvailable: 3}
            ], pageNumber: 0, pageSize: 100, totalElements: 4, totalPages: 3, first: true, last: false, hasNext: true, hasPrevious: false})
            .mockResolvedValueOnce({content: [
                {id: 3, name: 'Chạm ngưỡng', stockAvailable: 5},
                {id: 4, name: 'Đủ hàng', stockAvailable: 6}
            ], pageNumber: 1, pageSize: 100, totalElements: 4, totalPages: 3, first: false, last: false, hasNext: true, hasPrevious: true});

        const result = await InventoryDashboardService.getLowStockProducts(5);
        expect(result.map(product => product.id)).toEqual([1, 2, 3]);
        expect(mockedCatalog.getProducts).toHaveBeenCalledTimes(2);
        expect(mockedCatalog.getProducts).toHaveBeenLastCalledWith(expect.objectContaining({
            page: 1, active: true, sortBy: 'stockAvailable', sortDirection: 'ASC'
        }), undefined);
    });

    it('rejects an aborted request instead of applying the fallback', async () => {
        const controller = new AbortController();
        controller.abort();
        (apiClient.get as any).mockResolvedValue({value: '5'});
        await expect(InventoryDashboardService.getLowStockThreshold(controller.signal)).rejects.toMatchObject({name: 'AbortError'});
    });

    it('normalizes nullable or invalid stock to a safe numeric value', () => {
        expect(inventoryQuantity({id: 1, name: 'A', stockAvailable: null, stockRemain: 4})).toBe(4);
        expect(inventoryQuantity({id: 2, name: 'B', stockAvailable: undefined})).toBe(0);
    });
});
