import apiClient from '../api/api-client';
import {AdminCatalogService} from './admin-catalog.service';
import {describe, expect, it, jest} from '@jest/globals';

jest.mock('../api/api-client');

describe('AdminCatalogService.setProductActive', () => {
    it('uses the stable PATCH path and payload and returns the unwrapped detail', async () => {
        const detail = {id: 12, name: 'Mặt nạ', active: false};
        jest.mocked(apiClient.patch).mockResolvedValue(detail);

        await expect(AdminCatalogService.setProductActive(12, false, 'Tạm dừng bán')).resolves.toBe(detail);
        expect(apiClient.patch).toHaveBeenCalledWith('/api/admin/products/12/active', {
            active: false,
            reason: 'Tạm dừng bán'
        });
    });
});

describe('AdminCatalogService.getInventoryProducts', () => {
    it('uses the dedicated inventory path, server pagination, filters, sorting, and cancellation signal', async () => {
        const controller = new AbortController();
        const page = {content: [], pageNumber: 1, pageSize: 20, totalElements: 20, totalPages: 2};
        jest.mocked(apiClient.get).mockResolvedValue(page);

        await expect(AdminCatalogService.getInventoryProducts({
            search: 'serum', stockStatus: 'LOW_STOCK', page: 1, pageSize: 20,
            sortBy: 'stockAvailable', sortDirection: 'DESC'
        }, controller.signal)).resolves.toBe(page);

        expect(apiClient.get).toHaveBeenCalledWith('/api/admin/inventory', {
            params: {
                search: 'serum', stockStatus: 'LOW_STOCK', page: 1, pageSize: 20,
                sortBy: 'stockAvailable', sortDirection: 'DESC'
            },
            signal: controller.signal
        });
    });
});
