import { describe, expect, it, jest } from '@jest/globals';
import { ShippingService } from './shipping.service';

const mockApiPost = jest.fn();
const mockApiGet = jest.fn();
const mockApiPut = jest.fn();

jest.mock('../api/api-client', () => ({
    __esModule: true,
    default: {
        post: (...args: any[]) => mockApiPost(...args),
        get: (...args: any[]) => mockApiGet(...args),
        put: (...args: any[]) => mockApiPut(...args)
    }
}));

describe('ShippingService', () => {
    it('requests public shipping quotes with the checkout payload', () => {
        const payload = {
            customerAddress: '1 Nguyen Trai',
            customerCityId: 1,
            customerDistrictId: 2,
            customerWardId: 3,
            cartSubtotal: 350000,
            codAmount: 0,
            carrierId: 10,
            carrierServiceId: 20
        };

        ShippingService.getQuotes(payload);

        expect(mockApiPost).toHaveBeenCalledWith('/api/public/shipping/quotes', payload);
    });

    it('requests admin shipping carriers', () => {
        ShippingService.getAdminCarriers();

        expect(mockApiGet).toHaveBeenCalledWith('/api/admin/shipping/carriers');
    });

    it('requests admin carrier configuration', () => {
        ShippingService.getCarrierConfig();

        expect(mockApiGet).toHaveBeenCalledWith('/api/admin/shipping/carrier-config');
    });

    it('updates admin carrier configuration with the API doc payload', () => {
        const payload = {
            carrierId: 22151,
            standardService: 'VCN',
            expressService: 'VHT',
            expressFallbackId: 22384
        };

        ShippingService.updateCarrierConfig(payload);

        expect(mockApiPut).toHaveBeenCalledWith('/api/admin/shipping/carrier-config', payload);
    });
});
