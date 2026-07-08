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

const handleAuthError = (error: any) => {
    if (error?.response?.status === 401 || error?.response?.status === 403) {
        ToastService.error('Vui lòng đăng nhập để sử dụng giỏ hàng');
        return true;
    }
    return false;
};

interface CartState {
    items: CartItem[];
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
    getTotals: () => { subtotal: number; tax: number; total: number; itemCount: number };
}

export const useCartStore = create<CartState>((set, get) => ({
    items: [],
    isLoading: false,
    isSubmitting: false,
    checkoutError: null,
    lastCreatedOrder: null,
    pendingOrderKey: null,
    pendingOrderFingerprint: null,

    fetchCart: async () => {
        if (!authStorage.getAccessToken()) {
            set({ items: [], isLoading: false });
            return;
        }

        set({ isLoading: true });
        try {
            const response = await CustomerService.getCart();
            if (response.success) {
                const items = (response.data?.items || []).map(mapCartItemDto);
                set({ items, isLoading: false });
                return;
            }
            set({ isLoading: false });
        } catch {
            set({ isLoading: false });
        }
    },

    addToCart: async (product, quantity = 1) => {
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
                const items = (response.data?.items || []).map(mapCartItemDto);
                set({ items });
            }
            ToastService.success('Đã thêm sản phẩm vào giỏ hàng');
        } catch (error: any) {
            if (!handleAuthError(error)) {
                ToastService.error(getErrorMessage(error, 'Không thể thêm sản phẩm vào giỏ hàng'));
            }
        }
    },

    removeFromCart: async (productId) => {
        const previousItems = get().items;
        set({ items: previousItems.filter(item => item.product.id !== productId) });
        try {
            const response = await CustomerService.removeCartItem(productId);
            if (response.success) {
                ToastService.warning('Đã xóa sản phẩm khỏi giỏ hàng');
            }
        } catch (error: any) {
            set({ items: previousItems });
            if (!handleAuthError(error)) {
                ToastService.error(getErrorMessage(error, 'Không thể xóa sản phẩm'));
            }
        }
    },

    updateQuantity: async (productId, quantity) => {
        const safeQuantity = Math.max(1, quantity);
        const previousItems = get().items;
        set({
            items: previousItems.map(item =>
                item.product.id === productId
                    ? { ...item, quantity: safeQuantity }
                    : item
            )
        });
        try {
            await CustomerService.updateCartItem(productId, safeQuantity);
        } catch (error: any) {
            set({ items: previousItems });
            if (!handleAuthError(error)) {
                ToastService.error(getErrorMessage(error, 'Không thể cập nhật số lượng'));
            }
        }
    },

    clearCart: async () => {
        const previousItems = get().items;
        set({ items: [] });
        try {
            await CustomerService.clearCart();
        } catch (error: any) {
            set({ items: previousItems });
            if (!handleAuthError(error)) {
                ToastService.error(getErrorMessage(error, 'Không thể xóa giỏ hàng'));
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
                await CustomerService.clearCart();
            }

            set({
                ...(shouldClearCart ? { items: [] } : {}),
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
