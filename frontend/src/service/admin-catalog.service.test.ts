import {describe, expect, it, jest} from '@jest/globals';
import {AdminCatalogService} from './admin-catalog.service';

const mockGet = jest.fn();
const mockPost = jest.fn();
const mockPut = jest.fn();
const mockPatch = jest.fn();
const mockDelete = jest.fn();

jest.mock('../api/api-client', () => ({
    __esModule: true,
    default: {
        get: (...args: unknown[]) => mockGet(...args),
        post: (...args: unknown[]) => mockPost(...args),
        put: (...args: unknown[]) => mockPut(...args),
        patch: (...args: unknown[]) => mockPatch(...args),
        delete: (...args: unknown[]) => mockDelete(...args)
    }
}));

describe('AdminCatalogService inventory contract', () => {
    it('passes product filters and cancellation to the admin product API', () => {
        const controller = new AbortController();
        const params = {page: 1, pageSize: 100, active: true, inStock: false, sortBy: 'stockAvailable' as const, sortDirection: 'ASC' as const};
        AdminCatalogService.getProducts(params, controller.signal);
        expect(mockGet).toHaveBeenCalledWith('/api/admin/products', {params, signal: controller.signal});
    });

    it('uses the existing balance and ledger endpoints with cancellation', () => {
        const controller = new AbortController();
        AdminCatalogService.getInventoryBalance(10, controller.signal);
        AdminCatalogService.getInventoryLedger(10, controller.signal);
        expect(mockGet).toHaveBeenNthCalledWith(1, '/api/admin/inventory/10/balance', {signal: controller.signal});
        expect(mockGet).toHaveBeenNthCalledWith(2, '/api/admin/inventory/10/ledger', {signal: controller.signal});
    });

    it('sends opening stock and manual adjustment payloads unchanged', () => {
        const controller = new AbortController();
        AdminCatalogService.setOpeningStock(10, 12, 'Tồn đầu kỳ', controller.signal);
        AdminCatalogService.adjustInventory(10, 'CORRECTION', 9, 'Kiểm kê', controller.signal);
        expect(mockPost).toHaveBeenNthCalledWith(1, '/api/admin/inventory/10/opening',
            {quantity: 12, note: 'Tồn đầu kỳ'}, {signal: controller.signal});
        expect(mockPost).toHaveBeenNthCalledWith(2, '/api/admin/inventory/10/adjustments',
            {type: 'CORRECTION', quantity: 9, note: 'Kiểm kê'}, {signal: controller.signal});
    });
});
