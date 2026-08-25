import { describe, expect, it, jest } from '@jest/globals';
import { VoucherService } from './voucher.service';

const mockApiGet = jest.fn();
const mockApiPost = jest.fn();

jest.mock('../api/api-client', () => ({
    __esModule: true,
    default: {
        get: (...args: any[]) => mockApiGet(...args),
        post: (...args: any[]) => mockApiPost(...args)
    }
}));

describe('VoucherService', () => {
    it('loads active vouchers through the wrapped public contract', () => {
        VoucherService.getActive();
        expect(mockApiGet).toHaveBeenCalledWith('/api/public/vouchers/active');
    });

    it('requests a checkout preview with cancellation support', () => {
        const controller = new AbortController();
        const payload = {
            subtotal: 350000,
            shippingFee: 30000,
            items: [{ productId: 10, name: 'Áo hoodie', price: 350000, quantity: 1 }],
            autoApply: true
        };
        VoucherService.apply(payload, controller.signal);
        expect(mockApiPost).toHaveBeenCalledWith(
            '/api/public/vouchers/apply',
            payload,
            { signal: controller.signal }
        );
    });

    it('loads product vouchers with catalog pricing and cancellation support', () => {
        const controller = new AbortController();
        VoucherService.getForProduct(1001, {
            categoryId: 101,
            oldPrice: 229000,
            price: 189000
        }, controller.signal);

        expect(mockApiGet).toHaveBeenCalledWith('/api/public/vouchers/product/1001', {
            params: {
                categoryId: 101,
                oldPrice: 229000,
                price: 189000
            },
            signal: controller.signal
        });
    });
});
