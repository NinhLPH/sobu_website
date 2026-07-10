import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { CustomerService } from '../service/custom.service';
import { ProductModel } from '../interface/product.model';
import { useCartStore } from './useCartStore';
import { onlineCartRecovery } from '../utils/online-cart-recovery';

jest.mock('../service/custom.service');

const mockedCustomerService = jest.mocked(CustomerService);

const product: ProductModel = {
    id: '10',
    nhanhProductId: '10001',
    name: 'Ao hoodie',
    price: 350000,
    brand: 'SOBU',
    imageUrl: '',
    description: '',
    stock: 5
};

const shippingLocation = {
    customerCityName: 'Ha Noi',
    customerDistrictName: 'Ba Dinh',
    customerWardName: 'Phuc Xa',
    customerCityId: 254,
    customerDistrictId: 331,
    customerWardId: 1116
};

const shippingQuote = {
    carrierId: 10,
    carrierServiceId: 20,
    shippingFee: 30000
};

const onlinePayment = {
    id: 21,
    orderId: 12,
    paymentCode: 'SOBU-PAY-ONLINE',
    type: 'FULL' as const,
    paymentMethod: 'ONLINE' as const,
    status: 'PENDING' as const,
    amount: 350000,
    checkoutUrl: 'https://pay.payos.vn/web/checkout',
    createdAt: '2026-06-21T10:00:00',
    updatedAt: '2026-06-21T10:00:00'
};

const shippingQuoteRequiredMessage = 'Vui lòng chọn đơn vị giao hàng trước khi đặt hàng.';

const emptyCartResponse = {
    success: true,
    statusCode: 200,
    message: 'Cart retrieved',
    data: { items: [] }
};

const cartWithItem = (item: typeof product, quantity: number) => ({
    success: true,
    statusCode: 200,
    message: 'Item added to cart',
    data: {
        items: [{
            productId: item.id,
            nhanhProductId: item.nhanhProductId,
            name: item.name,
            price: item.price,
            imageUrl: item.imageUrl,
            quantity
        }]
    }
});

it('does not fetch the server cart when the store module is imported', () => {
    expect(mockedCustomerService.getCart).not.toHaveBeenCalled();
});

describe('useCartStore order submission', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        window.sessionStorage.clear();
        window.localStorage.clear();
        mockedCustomerService.getCart.mockResolvedValue(emptyCartResponse);
        mockedCustomerService.clearCart.mockResolvedValue({ success: true, statusCode: 200, message: 'Cart cleared', data: null as any });
        useCartStore.setState({
            items: [],
            isLoading: false,
            isSubmitting: false,
            checkoutError: null,
            lastCreatedOrder: null,
            pendingOrderKey: null,
            pendingOrderFingerprint: null
        });
    });

    it('does not call the cart API for guest sessions', async () => {
        useCartStore.setState({
            items: [{ product, quantity: 1 }],
            isLoading: true
        });

        await useCartStore.getState().fetchCart();

        expect(mockedCustomerService.getCart).not.toHaveBeenCalled();
        expect(useCartStore.getState().items).toEqual([]);
        expect(useCartStore.getState().isLoading).toBe(false);
    });

    it('loads the server cart when an access token exists', async () => {
        window.sessionStorage.setItem('accessToken', 'access-token');
        mockedCustomerService.getCart.mockResolvedValue(cartWithItem(product, 3));

        await useCartStore.getState().fetchCart();

        expect(mockedCustomerService.getCart).toHaveBeenCalledTimes(1);
        expect(useCartStore.getState().items).toEqual([{
            product: {
                id: product.id,
                nhanhProductId: product.nhanhProductId,
                name: product.name,
                price: product.price,
                imageUrl: product.imageUrl,
                brand: '',
                description: '',
                stock: 999
            },
            quantity: 3
        }]);
        expect(useCartStore.getState().isLoading).toBe(false);
    });

    it('restores a pending online cart when the server cart is empty', async () => {
        window.sessionStorage.setItem('accessToken', 'access-token');
        onlineCartRecovery.save(onlinePayment, [{ product, quantity: 2 }]);
        mockedCustomerService.getCart.mockResolvedValue(emptyCartResponse);

        await useCartStore.getState().fetchCart();

        expect(mockedCustomerService.getCart).toHaveBeenCalledTimes(1);
        expect(useCartStore.getState().items).toEqual([{
            product,
            quantity: 2
        }]);
        expect(useCartStore.getState().isLoading).toBe(false);
    });

    it('clears pending online cart recovery when the customer changes the cart', async () => {
        mockedCustomerService.addCartItem.mockResolvedValue(cartWithItem(product, 2));
        mockedCustomerService.updateCartItem.mockResolvedValue({ success: true, statusCode: 200, message: 'Cart updated', data: null as any });
        mockedCustomerService.removeCartItem.mockResolvedValue({ success: true, statusCode: 200, message: 'Item removed', data: null as any });

        onlineCartRecovery.save(onlinePayment, [{ product, quantity: 1 }]);
        await useCartStore.getState().addToCart(product, 2);
        expect(onlineCartRecovery.get()).toBeNull();

        onlineCartRecovery.save(onlinePayment, [{ product, quantity: 2 }]);
        useCartStore.setState({ items: [{ product, quantity: 2 }] });
        await useCartStore.getState().updateQuantity(product.id, 3);
        expect(onlineCartRecovery.get()).toBeNull();

        onlineCartRecovery.save(onlinePayment, [{ product, quantity: 3 }]);
        useCartStore.setState({ items: [{ product, quantity: 3 }] });
        await useCartStore.getState().removeFromCart(product.id);
        expect(onlineCartRecovery.get()).toBeNull();

        onlineCartRecovery.save(onlinePayment, [{ product, quantity: 3 }]);
        useCartStore.setState({ items: [{ product, quantity: 3 }] });
        await useCartStore.getState().clearCart();
        expect(onlineCartRecovery.get()).toBeNull();
    });

    it('creates the API payload from cart items and clears the cart on success', async () => {
        mockedCustomerService.addCartItem.mockResolvedValue(cartWithItem(product, 2));
        mockedCustomerService.createOrder.mockResolvedValue({
            success: true,
            statusCode: 201,
            message: 'Order created',
            data: {
                id: 1,
                orderCode: 'ORD-001',
                status: 'NEW',
                syncStatus: 'PENDING',
                totalAmount: 700000,
                items: []
            }
        });

        await useCartStore.getState().addToCart(product, 2);

        const order = await useCartStore.getState().submitOrder({
            customerName: 'Nguyen Van A',
            customerMobile: '0901234567',
            ...shippingLocation,
            ...shippingQuote
        });

        expect(mockedCustomerService.createOrder).toHaveBeenCalledWith(
            expect.objectContaining({
                customerName: 'Nguyen Van A',
                customerMobile: '0901234567',
                ...shippingLocation,
                ...shippingQuote,
                items: [{
                    nhanhProductId: '10001',
                    name: 'Ao hoodie',
                    price: 350000,
                    discount: 0,
                    quantity: 2
                }]
            }),
            expect.any(String)
        );
        expect(mockedCustomerService.clearCart).toHaveBeenCalled();
        expect(order.id).toBe(1);
        expect(useCartStore.getState().items).toEqual([]);
        expect(useCartStore.getState().checkoutError).toBeNull();
    });

    it('keeps cart items and exposes the backend message when creation fails', async () => {
        mockedCustomerService.addCartItem.mockResolvedValue(cartWithItem(product, 1));
        mockedCustomerService.createOrder.mockRejectedValue({
            response: {
                status: 409,
                data: { message: 'Idempotency conflict' }
            }
        });

        await useCartStore.getState().addToCart(product);

        await expect(useCartStore.getState().submitOrder({
            customerName: 'Nguyen Van A',
            customerMobile: '0901234567',
            ...shippingLocation,
            ...shippingQuote
        })).rejects.toBeDefined();

        expect(useCartStore.getState().items).toHaveLength(1);
        expect(useCartStore.getState().checkoutError).toBe('Idempotency conflict');
        expect(useCartStore.getState().isSubmitting).toBe(false);
    });

    it('allows order submission when only shipping fee is present', async () => {
        mockedCustomerService.addCartItem.mockResolvedValue(cartWithItem(product, 1));
        mockedCustomerService.createOrder.mockResolvedValue({
            success: true,
            statusCode: 201,
            message: 'Order created',
            data: {
                id: 2,
                orderCode: 'ORD-002',
                status: 'NEW',
                syncStatus: 'PENDING',
                totalAmount: 380000,
                items: []
            }
        });

        await useCartStore.getState().addToCart(product);

        await useCartStore.getState().submitOrder({
            customerName: 'Nguyen Van A',
            customerMobile: '0901234567',
            ...shippingLocation,
            shippingFee: 30000
        });

        expect(mockedCustomerService.createOrder).toHaveBeenCalledWith(
            expect.objectContaining({
                ...shippingLocation,
                shippingFee: 30000
            }),
            expect.any(String)
        );
        expect(mockedCustomerService.createOrder.mock.calls[0][0]).not.toHaveProperty('carrierId');
        expect(mockedCustomerService.createOrder.mock.calls[0][0]).not.toHaveProperty('carrierServiceId');
    });

    it('rejects order submission when shipping quote fields are missing', async () => {
        mockedCustomerService.addCartItem.mockResolvedValue(cartWithItem(product, 1));
        await useCartStore.getState().addToCart(product);

        await expect(useCartStore.getState().submitOrder({
            customerName: 'Nguyen Van A',
            customerMobile: '0901234567',
            ...shippingLocation
        })).rejects.toThrow(shippingQuoteRequiredMessage);

        expect(mockedCustomerService.createOrder).not.toHaveBeenCalled();
        expect(useCartStore.getState().checkoutError).toBe(shippingQuoteRequiredMessage);
    });

    it.each([
        ['missing shipping fee', { carrierId: 10, carrierServiceId: 20 }],
        ['non-finite shipping fee', { carrierId: 10, carrierServiceId: 20, shippingFee: Number.NaN }],
        ['negative shipping fee', { carrierId: 10, carrierServiceId: 20, shippingFee: -1 }]
    ])('rejects order submission with %s', async (_label, invalidQuote) => {
        mockedCustomerService.addCartItem.mockResolvedValue(cartWithItem(product, 1));
        await useCartStore.getState().addToCart(product);

        await expect(useCartStore.getState().submitOrder({
            customerName: 'Nguyen Van A',
            customerMobile: '0901234567',
            ...shippingLocation,
            ...invalidQuote
        })).rejects.toThrow(shippingQuoteRequiredMessage);

        expect(mockedCustomerService.createOrder).not.toHaveBeenCalled();
        expect(useCartStore.getState().checkoutError).toBe(shippingQuoteRequiredMessage);
    });
});
