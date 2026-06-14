import { create } from 'zustand';
import { CartItem, ProductModel } from '../interface/product.model';
import {
    CreateNormalOrderDto,
    OrderShippingLocationDto,
    OrderResponseDto
} from '../interface/order.model';
import { CustomerService } from '../service/custom.service';
import { createIdempotencyKey } from '../utils/idempotency';

type CheckoutDetails =
    Omit<CreateNormalOrderDto, 'items' | keyof OrderShippingLocationDto>
    & OrderShippingLocationDto;

const getErrorMessage = (error: any, fallback: string) =>
    error?.response?.data?.message ||
    error?.response?.data?.error ||
    error?.message ||
    fallback;

interface CartState {
    items: CartItem[];
    isSubmitting: boolean;
    checkoutError: string | null;
    lastCreatedOrder: OrderResponseDto | null;
    pendingOrderKey: string | null;
    pendingOrderFingerprint: string | null;
    addToCart: (product: ProductModel, quantity?: number) => void;
    removeFromCart: (productId: string) => void;
    updateQuantity: (productId: string, quantity: number) => void;
    clearCart: () => void;
    clearCheckoutError: () => void;
    submitOrder: (details: CheckoutDetails) => Promise<OrderResponseDto>;
    getTotals: () => { subtotal: number; tax: number; total: number; itemCount: number };
}

export const useCartStore = create<CartState>((set, get) => ({
    items: [],
    isSubmitting: false,
    checkoutError: null,
    lastCreatedOrder: null,
    pendingOrderKey: null,
    pendingOrderFingerprint: null,

    addToCart: (product, quantity = 1) => {
        set((state) => {
            const existingItem = state.items.find(item => item.product.id === product.id);
            const items = existingItem
                ? state.items.map(item =>
                    item.product.id === product.id
                        ? { ...item, quantity: item.quantity + quantity }
                        : item
                )
                : [...state.items, { product, quantity }];

            return {
                items,
                pendingOrderKey: null,
                pendingOrderFingerprint: null
            };
        });
    },

    removeFromCart: (productId) => {
        set((state) => ({
            items: state.items.filter(item => item.product.id !== productId),
            pendingOrderKey: null,
            pendingOrderFingerprint: null
        }));
    },

    updateQuantity: (productId, quantity) => {
        set((state) => ({
            items: state.items.map(item =>
                item.product.id === productId
                    ? { ...item, quantity: Math.max(1, quantity) }
                    : item
            ),
            pendingOrderKey: null,
            pendingOrderFingerprint: null
        }));
    },

    clearCart: () => set({
        items: [],
        pendingOrderKey: null,
        pendingOrderFingerprint: null
    }),

    clearCheckoutError: () => set({ checkoutError: null }),

    submitOrder: async (details) => {
        const items = get().items;
        if (items.length === 0) {
            const message = 'Giỏ hàng đang trống.';
            set({ checkoutError: message });
            throw new Error(message);
        }

        const hasValidShippingLocation = [
            details.customerCityName,
            details.customerDistrictName,
            details.customerWardName
        ].every((name) => name.trim().length > 0) && [
            details.customerCityId,
            details.customerDistrictId,
            details.customerWardId
        ].every((id) => Number.isInteger(id) && id > 0);

        if (!hasValidShippingLocation) {
            const message = 'Vui lòng chọn đầy đủ tỉnh/thành phố, quận/huyện và phường/xã.';
            set({ checkoutError: message });
            throw new Error(message);
        }

        const payload: CreateNormalOrderDto = {
            ...details,
            items: items.map(({ product, quantity }) => ({
                nhanhProductId: product.nhanhProductId || product.id,
                name: product.name,
                price: product.price,
                discount: 0,
                quantity
            }))
        };
        const fingerprint = JSON.stringify(payload);
        const state = get();
        const idempotencyKey = state.pendingOrderFingerprint === fingerprint
            && state.pendingOrderKey
            ? state.pendingOrderKey
            : createIdempotencyKey();

        set({
            isSubmitting: true,
            checkoutError: null,
            pendingOrderKey: idempotencyKey,
            pendingOrderFingerprint: fingerprint
        });

        try {
            const response = await CustomerService.createOrder(payload, idempotencyKey);
            if (!response.success) {
                throw new Error(response.message || 'Không thể tạo đơn hàng.');
            }

            set({
                items: [],
                isSubmitting: false,
                checkoutError: null,
                lastCreatedOrder: response.data,
                pendingOrderKey: null,
                pendingOrderFingerprint: null
            });
            return response.data;
        } catch (error) {
            const message = getErrorMessage(error, 'Không thể tạo đơn hàng. Vui lòng thử lại.');
            set({ isSubmitting: false, checkoutError: message });
            throw error;
        }
    },

    getTotals: () => {
        const items = get().items;
        const subtotal = items.reduce((sum, item) => sum + item.product.price * item.quantity, 0);
        const tax = subtotal * 0.1;
        const total = subtotal + tax;
        const itemCount = items.reduce((sum, item) => sum + item.quantity, 0);
        return { subtotal, tax, total, itemCount };
    }
}));
