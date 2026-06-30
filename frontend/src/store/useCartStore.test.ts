import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { CustomerService } from '../service/custom.service';
import { ProductModel } from '../interface/product.model';
import { useCartStore } from './useCartStore';

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
            ...shippingLocation
        });

        expect(mockedCustomerService.createOrder).toHaveBeenCalledWith(
            expect.objectContaining({
                customerName: 'Nguyen Van A',
                customerMobile: '0901234567',
                ...shippingLocation,
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
            ...shippingLocation
        })).rejects.toBeDefined();

        expect(useCartStore.getState().items).toHaveLength(1);
        expect(useCartStore.getState().checkoutError).toBe('Idempotency conflict');
        expect(useCartStore.getState().isSubmitting).toBe(false);
    });
});
