import { describe, expect, it, jest } from '@jest/globals';
import { ShippingService } from './shipping.service';

const mockApiPost = jest.fn();

jest.mock('../api/api-client', () => ({
    __esModule: true,
    default: {
        post: (...args: any[]) => mockApiPost(...args)
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
            codAmount: 0
        };

        ShippingService.getQuotes(payload);

        expect(mockApiPost).toHaveBeenCalledWith('/api/public/shipping/quotes', payload);
    });
});
