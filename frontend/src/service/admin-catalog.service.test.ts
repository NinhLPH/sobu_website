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
