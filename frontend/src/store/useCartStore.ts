import { create } from 'zustand';
import { CartItem, ProductModel } from '../interface/product.model';
import {
    CreateNormalOrderDto,
    OrderShippingLocationDto,
    OrderResponseDto
} from '../interface/order.model';
import { CustomerService } from '../service/custom.service';
import {ToastService} from "../service/toast.service";
import { createIdempotencyKey } from '../utils/idempotency';
import { CartItemDto } from '../interface/cart.dto';
import { authStorage } from '../utils/auth-storage';
import { onlineCartRecovery } from '../utils/online-cart-recovery';
import { cartFallback } from '../utils/cart-fallback';

type CheckoutDetails =
    Omit<CreateNormalOrderDto, 'items' | keyof OrderShippingLocationDto>
    & OrderShippingLocationDto;

interface SubmitOrderOptions {
    clearCartOnSuccess?: boolean;
}

const getErrorMessage = (error: any, fallback: string) =>
    error?.response?.data?.message ||
    error?.response?.data?.error ||
    error?.message ||
    fallback;

const mapCartItemDto = (dto: CartItemDto): CartItem => ({
    product: {
        id: dto.productId,
        nhanhProductId: dto.nhanhProductId,
        name: dto.name,
        price: dto.price,
        imageUrl: dto.imageUrl || '',
        brand: '',
        description: '',
        stock: 999,
    },
    quantity: dto.quantity,
});

const isAuthError = (error: any) =>
    error?.response?.status === 401 || error?.response?.status === 403;

const handleAuthError = (error: any) => {
    if (isAuthError(error)) {
        ToastService.error('Vui lòng đăng nhập để sử dụng giỏ hàng');
        return true;
    }
    return false;
};

interface CartState {
    items: CartItem[];
    isUsingFallback: boolean;
    isLoading: boolean;
    isSubmitting: boolean;
    checkoutError: string | null;
    lastCreatedOrder: OrderResponseDto | null;
    pendingOrderKey: string | null;
    pendingOrderFingerprint: string | null;
    fetchCart: () => Promise<void>;
    addToCart: (product: ProductModel, quantity?: number) => Promise<void>;
    removeFromCart: (productId: string) => Promise<void>;
    updateQuantity: (productId: string, quantity: number) => Promise<void>;
    clearCart: () => Promise<void>;
    clearCheckoutError: () => void;
    submitOrder: (details: CheckoutDetails, options?: SubmitOrderOptions) => Promise<OrderResponseDto>;
    restorePendingOnlineCart: () => boolean;
    getTotals: () => { subtotal: number; tax: number; total: number; itemCount: number };
}

export const useCartStore = create<CartState>((set, get) => ({
    items: [],
    isUsingFallback: false,
    isLoading: false,
    isSubmitting: false,
    checkoutError: null,
    lastCreatedOrder: null,
    pendingOrderKey: null,
    pendingOrderFingerprint: null,

    fetchCart: async () => {
        if (!authStorage.getAccessToken()) {
            set({ items: [], isLoading: false, isUsingFallback: false });
            return;
        }

        const fallbackItems = cartFallback.get();
        if (fallbackItems !== null) {
            set({ items: fallbackItems, isLoading: false, isUsingFallback: true });
            return;
        }

        set({ isLoading: true });
        try {
            const response = await CustomerService.getCart();
            if (response.success) {
                const serverItems = (response.data?.items || []).map(mapCartItemDto);
                const pendingOnlineCart = onlineCartRecovery.get();
                const items = serverItems.length > 0
                    ? serverItems
                    : pendingOnlineCart?.items ?? [];
                set({ items, isLoading: false, isUsingFallback: false });
                return;
            }
            set({ items: [], isLoading: false, isUsingFallback: true });
            cartFallback.save([]);
        } catch (error) {
            if (isAuthError(error)) {
                set({ isLoading: false });
                return;
            }

            set({ items: [], isLoading: false, isUsingFallback: true });
            cartFallback.save([]);
        }
    },

    addToCart: async (product, quantity = 1) => {
        const addToFallback = () => {
            const items = [
                ...get().items.filter(item => item.product.id !== product.id),
                { product, quantity }
            ];
            onlineCartRecovery.clear();
            cartFallback.save(items);
            set({ items, isUsingFallback: true });
        };

        if (get().isUsingFallback) {
            addToFallback();
            ToastService.success('Đã thêm sản phẩm vào giỏ hàng');
            return;
        }

        try {
            const response = await CustomerService.addCartItem({
                productId: product.id,
                nhanhProductId: product.nhanhProductId,
                name: product.name,
                price: product.price,
                imageUrl: product.imageUrl,
                quantity
            });
            if (response.success) {
                onlineCartRecovery.clear();
                const items = (response.data?.items || []).map(mapCartItemDto);
                set({ items });
            } else {
                addToFallback();
            }
            ToastService.success('Đã thêm sản phẩm vào giỏ hàng');
        } catch (error: any) {
            if (!handleAuthError(error)) {
                addToFallback();
                ToastService.success('Đã thêm sản phẩm vào giỏ hàng');
            }
        }
    },

    removeFromCart: async (productId) => {
        const previousItems = get().items;
        const items = previousItems.filter(item => item.product.id !== productId);
        set({ items });

        const removeFromFallback = () => {
            onlineCartRecovery.clear();
            cartFallback.save(items);
            set({ isUsingFallback: true });
        };

        if (get().isUsingFallback) {
            removeFromFallback();
            ToastService.warning('Đã xóa sản phẩm khỏi giỏ hàng');
            return;
        }

        try {
            const response = await CustomerService.removeCartItem(productId);
            if (response.success) {
                onlineCartRecovery.clear();
            } else {
                removeFromFallback();
            }
            ToastService.warning('Đã xóa sản phẩm khỏi giỏ hàng');
        } catch (error: any) {
            if (handleAuthError(error)) {
                set({ items: previousItems });
            } else {
                removeFromFallback();
                ToastService.warning('Đã xóa sản phẩm khỏi giỏ hàng');
            }
        }
    },

    updateQuantity: async (productId, quantity) => {
        const safeQuantity = Math.max(1, quantity);
        const previousItems = get().items;
        const items = previousItems.map(item =>
            item.product.id === productId
                ? { ...item, quantity: safeQuantity }
                : item
        );
        set({ items });

        const updateFallback = () => {
            onlineCartRecovery.clear();
            cartFallback.save(items);
            set({ isUsingFallback: true });
        };

        if (get().isUsingFallback) {
            updateFallback();
            return;
        }

        try {
            const response = await CustomerService.updateCartItem(productId, safeQuantity);
            if (response.success) {
                onlineCartRecovery.clear();
            } else {
                updateFallback();
            }
        } catch (error: any) {
            if (handleAuthError(error)) {
                set({ items: previousItems });
            } else {
                updateFallback();
            }
        }
    },

    clearCart: async () => {
        const previousItems = get().items;
        set({ items: [] });

        const clearFallback = () => {
            onlineCartRecovery.clear();
            cartFallback.save([]);
            set({ isUsingFallback: true });
        };

        if (get().isUsingFallback) {
            clearFallback();
            return;
        }

        try {
            const response = await CustomerService.clearCart();
            if (response.success) {
                onlineCartRecovery.clear();
            } else {
                clearFallback();
            }
        } catch (error: any) {
            if (handleAuthError(error)) {
                set({ items: previousItems });
            } else {
                clearFallback();
            }
        }
    },

    clearCheckoutError: () => set({ checkoutError: null }),

    submitOrder: async (details, options = {}) => {
        const shouldClearCart = options.clearCartOnSuccess !== false;
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

        const hasValidShippingFee = typeof details.shippingFee === 'number'
            && Number.isFinite(details.shippingFee)
            && details.shippingFee >= 0;

        if (!hasValidShippingFee) {
            const message = 'Vui lòng chọn đơn vị giao hàng trước khi đặt hàng.';
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

            if (shouldClearCart) {
                if (get().isUsingFallback) {
                    cartFallback.clear();
                } else {
                    await CustomerService.clearCart();
                }
                onlineCartRecovery.clear();
            }

            set({
                ...(shouldClearCart ? { items: [] } : {}),
                ...(shouldClearCart ? { isUsingFallback: false } : {}),
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

    restorePendingOnlineCart: () => {
        const pendingOnlineCart = onlineCartRecovery.get();
        if (!pendingOnlineCart) {
            return false;
        }
        set({ items: pendingOnlineCart.items });
        return true;
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
