import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { CustomerService } from '../service/custom.service';
import { useCartStore } from './useCartStore';
import { ProductModel } from '../interface/product.model';
import { cartFallback } from '../utils/cart-fallback';
import { ToastService } from '../service/toast.service';
import { PublicCatalogService } from '../service/public-catalog.service';

jest.mock('../service/custom.service');
jest.mock('../service/public-catalog.service');
jest.mock('../service/toast.service');

const api = jest.mocked(CustomerService);
const product: ProductModel = {
    id: '10', name: 'Audit product', price: 100000, stock: 5,
    brand: '', imageUrl: '', description: ''
};
const details = {
    customerName: 'Audit Customer', customerMobile: '0900000001',
    customerCityName: 'Ho Chi Minh', customerWardName: 'Test Ward',
    customerCityId: 79, customerWardId: 27154,
    carrierId: 10, carrierServiceId: 20, shippingFee: 30000
};

// Regressions for successful checkout cleanup and transient cart read failures.
describe('Customer checkout audit', () => {
    beforeEach(() => {
        jest.resetAllMocks();
        window.sessionStorage.clear();
        window.localStorage.clear();
        window.sessionStorage.setItem('accessToken', 'audit-token');
        window.sessionStorage.setItem('user', JSON.stringify({ id: 990, email: 'audit@example.test' }));
        useCartStore.setState({
            items: [{ product, quantity: 1 }], isUsingFallback: false,
            fallbackSource: null, cartOwnerId: null, cartLoadError: null, hasLegacyEmptyCart: false,
            isHydratingProducts: false, hydrationError: null,
            isLoading: false, isSubmitting: false, checkoutError: null,
            lastCreatedOrder: null, pendingOrderKey: null, pendingOrderFingerprint: null
        });
    });

    it('returns an already-created order even if clearing the cart fails', async () => {
        const order = {
            id: 990, orderCode: 'AUDIT-990', status: 'NEW' as const,
            syncStatus: 'PENDING' as const, totalAmount: 130000, items: []
        };
        api.createOrder.mockResolvedValue({ success: true, statusCode: 201, message: 'Created', data: order });
        api.clearCart.mockRejectedValue(new Error('Redis unavailable'));

        await expect(useCartStore.getState().submitOrder(details)).resolves.toEqual(order);
        expect(useCartStore.getState().lastCreatedOrder).toEqual(order);
        expect(useCartStore.getState().checkoutError).toBeNull();
        expect(cartFallback.getSnapshot()).toEqual({ items: [], source: 'local-edit' });
        await useCartStore.getState().fetchCart();
        expect(api.getCart).not.toHaveBeenCalled();
        expect(useCartStore.getState().items).toEqual([]);
    });

    it('preserves existing cart items when a refresh has a transient failure', async () => {
        api.getCart.mockRejectedValue(new Error('Temporary network failure'));

        await useCartStore.getState().fetchCart();

        expect(useCartStore.getState().items).toEqual([{ product, quantity: 1 }]);
    });

    it('can recover the server cart after a transient outage', async () => {
        api.getCart.mockRejectedValueOnce(new Error('Temporary outage'));
        await useCartStore.getState().fetchCart();
        api.getCart.mockResolvedValue({ success: true, statusCode: 200, message: 'Recovered', data: {
            items: [{ productId: '10', name: product.name, price: product.price, quantity: 1 }]
        } });

        await useCartStore.getState().fetchCart();

        expect(api.getCart).toHaveBeenCalledTimes(2);
        expect(useCartStore.getState().items).toHaveLength(1);
        expect(useCartStore.getState().cartLoadError).toBeNull();
        expect(cartFallback.get()).toBeNull();
    });

    it('records success before cleanup settles and handles a false cleanup response', async () => {
        const order = { id: 991, orderCode: 'AUDIT-991', status: 'NEW' as const,
            syncStatus: 'PENDING' as const, totalAmount: 130000, items: [] };
        api.createOrder.mockResolvedValue({ success: true, statusCode: 201, message: 'Created', data: order });
        let finishCleanup!: (value: any) => void;
        let signalCleanup!: () => void;
        const cleanupStarted = new Promise<void>(resolve => { signalCleanup = resolve; });
        api.clearCart.mockImplementation(() => new Promise(resolve => {
            finishCleanup = resolve;
            signalCleanup();
        }));
        const pending = useCartStore.getState().submitOrder(details);
        await cleanupStarted;
        expect(useCartStore.getState()).toEqual(expect.objectContaining({
            lastCreatedOrder: order, isSubmitting: true, pendingOrderKey: null, checkoutError: null
        }));
        finishCleanup({ success: false, message: 'Unavailable' });
        await expect(pending).resolves.toEqual(order);
        expect(useCartStore.getState().isSubmitting).toBe(false);
        expect(ToastService.warning).toHaveBeenCalledWith('Đơn hàng đã được tạo. Giỏ hàng trên máy chủ chưa được cập nhật.');
        expect(api.createOrder).toHaveBeenCalledTimes(1);
        expect(api.clearCart).toHaveBeenCalledTimes(1);
    });

    it('distinguishes failed initial loading from a confirmed empty server cart', async () => {
        useCartStore.setState({ items: [] });
        api.getCart.mockResolvedValueOnce({ success: false, message: 'Unavailable' } as any)
            .mockResolvedValueOnce({ success: true, data: { items: [] } } as any);
        await useCartStore.getState().fetchCart();
        expect(useCartStore.getState().cartLoadError).toBe('Unavailable');
        expect(cartFallback.getSnapshot()?.source).toBe('read-cache');
        await useCartStore.getState().fetchCart();
        expect(useCartStore.getState()).toEqual(expect.objectContaining({
            items: [], cartLoadError: null, isUsingFallback: false
        }));
    });

    it('retries a read-cache snapshot after a store reload', async () => {
        cartFallback.save([{ product, quantity: 2 }], 'read-cache');
        useCartStore.setState({ items: [] });
        api.getCart.mockRejectedValue(new Error('Offline'));
        await useCartStore.getState().fetchCart();
        expect(api.getCart).toHaveBeenCalledTimes(1);
        expect(useCartStore.getState().items[0].quantity).toBe(2);
    });

    it('does not overwrite local edits or an intentional empty cart on refresh', async () => {
        api.getCart.mockRejectedValue(new Error('Offline'));
        await useCartStore.getState().fetchCart();
        await useCartStore.getState().updateQuantity('10', 3);
        await useCartStore.getState().fetchCart();
        expect(useCartStore.getState().items[0].quantity).toBe(3);
        await useCartStore.getState().clearCart();
        await useCartStore.getState().fetchCart({ recoverLegacyEmpty: true });
        expect(api.getCart).toHaveBeenCalledTimes(1);
        expect(cartFallback.getSnapshot()).toEqual({ items: [], source: 'local-edit' });
    });

    it('replaces legacy empty data only after an explicit successful recovery', async () => {
        window.sessionStorage.setItem('sobu.cartFallback.v1:990', JSON.stringify({ items: [] }));
        await useCartStore.getState().fetchCart();
        expect(api.getCart).not.toHaveBeenCalled();
        expect(useCartStore.getState().hasLegacyEmptyCart).toBe(true);
        api.getCart.mockRejectedValueOnce(new Error('Offline'));
        await useCartStore.getState().fetchCart({ recoverLegacyEmpty: true });
        expect(cartFallback.getSnapshot()?.source).toBe('legacy-empty');
        api.getCart.mockResolvedValueOnce({ success: true, data: { items: [] } } as any);
        await useCartStore.getState().fetchCart({ recoverLegacyEmpty: true });
        expect(cartFallback.get()).toBeNull();
        expect(useCartStore.getState().hasLegacyEmptyCart).toBe(false);
    });

    it('does not turn an authentication failure into offline cart data', async () => {
        api.getCart.mockRejectedValue({ response: { status: 403 } });
        await useCartStore.getState().fetchCart();
        expect(useCartStore.getState().items).toEqual([{ product, quantity: 1 }]);
        expect(useCartStore.getState().isUsingFallback).toBe(false);
        expect(cartFallback.get()).toBeNull();
    });

    it('ignores a fetch result after the customer edits the cart', async () => {
        let finishRead!: (value: any) => void;
        api.getCart.mockImplementation(() => new Promise(resolve => { finishRead = resolve; }));
        const pending = useCartStore.getState().fetchCart();
        api.updateCartItem.mockRejectedValue(new Error('Offline'));
        await useCartStore.getState().updateQuantity('10', 4);
        finishRead({ success: true, data: { items: [] } });
        await pending;
        expect(useCartStore.getState().items[0].quantity).toBe(4);
        expect(cartFallback.getSnapshot()?.source).toBe('local-edit');
        expect(useCartStore.getState().isLoading).toBe(false);
    });

    it('ignores previous-account results and never caches their items for the new account', async () => {
        let finishRead!: (value: any) => void;
        api.getCart.mockImplementationOnce(() => new Promise(resolve => { finishRead = resolve; }));
        const pending = useCartStore.getState().fetchCart();
        window.sessionStorage.setItem('user', JSON.stringify({ id: 992 }));
        api.getCart.mockRejectedValueOnce(new Error('Offline'));
        await useCartStore.getState().fetchCart();
        finishRead({ success: true, data: { items: [{ productId: '10', quantity: 8 }] } });
        await pending;
        expect(useCartStore.getState().items).toEqual([]);
        expect(cartFallback.getSnapshot()).toEqual({ items: [], source: 'read-cache' });
    });

    it('does not apply an older fetch after a newer fetch has succeeded', async () => {
        let finishOldRead!: (value: any) => void;
        api.getCart.mockImplementationOnce(() => new Promise(resolve => { finishOldRead = resolve; }))
            .mockResolvedValueOnce({ success: true, data: { items: [] } } as any);
        const oldRead = useCartStore.getState().fetchCart();
        await useCartStore.getState().fetchCart();
        finishOldRead({ success: true, data: { items: [{ productId: '10', quantity: 9 }] } });
        await oldRead;
        expect(useCartStore.getState().items).toEqual([]);
    });

    it('discards a pending read if the customer logs out', async () => {
        let finishRead!: (value: any) => void;
        api.getCart.mockImplementationOnce(() => new Promise(resolve => { finishRead = resolve; }));
        const pending = useCartStore.getState().fetchCart();
        window.sessionStorage.clear();
        finishRead({ success: true, data: { items: [{ productId: '10', quantity: 9 }] } });
        await pending;
        expect(useCartStore.getState().items).toEqual([]);
        expect(useCartStore.getState().isLoading).toBe(false);
    });

    it('ignores stale product hydration after a quantity change', async () => {
        let finishDetail!: (value: any) => void;
        jest.mocked(PublicCatalogService).getProductDetail.mockImplementation(
            () => new Promise(resolve => { finishDetail = resolve; }));
        const pending = useCartStore.getState().hydrateProducts();
        api.updateCartItem.mockRejectedValue(new Error('Offline'));
        await useCartStore.getState().updateQuantity('10', 4);
        finishDetail({ id: 10, name: 'Updated', price: 120000, images: [] });
        await pending;
        expect(useCartStore.getState().items[0].quantity).toBe(4);
        expect(useCartStore.getState().isHydratingProducts).toBe(false);
    });

    it('keeps read recovery working if saving browser storage fails', async () => {
        const storageWrite = jest.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('Storage denied'); });
        try {
            api.getCart.mockRejectedValueOnce(new Error('Offline'))
                .mockResolvedValueOnce({ success: true, data: { items: [] } } as any);
            await useCartStore.getState().fetchCart();
            expect(useCartStore.getState().items).toHaveLength(1);
            await useCartStore.getState().fetchCart();
            expect(api.getCart).toHaveBeenCalledTimes(2);
            expect(useCartStore.getState().cartLoadError).toBeNull();
        } finally { storageWrite.mockRestore(); }
    });
});
