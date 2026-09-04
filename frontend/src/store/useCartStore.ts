import { create } from 'zustand';
import {
    CartItem,
    mapDetailToProductModel,
    ProductModel
} from '../interface/product.model';
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
import { cartFallback, CartFallbackSource } from '../utils/cart-fallback';
import { PublicCatalogService } from '../service/public-catalog.service';
import { useIntegrationStore } from './useIntegrationStore';

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
    fallbackSource: CartFallbackSource | null;
    cartOwnerId: string | null;
    cartLoadError: string | null;
    hasLegacyEmptyCart: boolean;
    isLoading: boolean;
    isSubmitting: boolean;
    isHydratingProducts: boolean;
    hydrationError: string | null;
    checkoutError: string | null;
    lastCreatedOrder: OrderResponseDto | null;
    pendingOrderKey: string | null;
    pendingOrderFingerprint: string | null;
    fetchCart: (options?: { recoverLegacyEmpty?: boolean }) => Promise<void>;
    hydrateProducts: () => Promise<void>;
    addToCart: (product: ProductModel, quantity?: number) => Promise<void>;
    removeFromCart: (productId: string) => Promise<void>;
    updateQuantity: (productId: string, quantity: number) => Promise<void>;
    clearCart: () => Promise<void>;
    clearCheckoutError: () => void;
    submitOrder: (details: CheckoutDetails, options?: SubmitOrderOptions) => Promise<OrderResponseDto>;
    restorePendingOnlineCart: () => boolean;
    getTotals: () => { subtotal: number; tax: number; total: number; itemCount: number };
}

const currentCartOwner = (): string | null => {
    const token = authStorage.getAccessToken();
    return token ? String(authStorage.getUser()?.id ?? token) : null;
};

export const useCartStore = create<CartState>((set, get) => {
    let cartRevision = 0;
    let fetchSequence = 0;
    let hydrationSequence = 0;

    const prepareCartMutation = () => {
        cartRevision++;
        hydrationSequence++;
        const owner = currentCartOwner();
        const changedOwner = get().cartOwnerId !== null && get().cartOwnerId !== owner;
        set({
            ...(changedOwner ? { items: [], isUsingFallback: false, fallbackSource: null } : {}),
            cartOwnerId: owner, isLoading: false, isHydratingProducts: false,
            cartLoadError: null, hasLegacyEmptyCart: false
        });
    };

    return ({
    items: [],
    isUsingFallback: false,
    fallbackSource: null,
    cartOwnerId: null,
    cartLoadError: null,
    hasLegacyEmptyCart: false,
    isLoading: false,
    isSubmitting: false,
    isHydratingProducts: false,
    hydrationError: null,
    checkoutError: null,
    lastCreatedOrder: null,
    pendingOrderKey: null,
    pendingOrderFingerprint: null,

    fetchCart: async (options = {}) => {
        const sequence = ++fetchSequence;
        const revision = cartRevision;
        const owner = currentCartOwner();
        hydrationSequence++;
        set({ isHydratingProducts: false });
        if (!owner) {
            set({ items: [], isLoading: false, isUsingFallback: false, fallbackSource: null,
                cartOwnerId: null, cartLoadError: null, hasLegacyEmptyCart: false });
            return;
        }

        if (get().cartOwnerId !== null && get().cartOwnerId !== owner) {
            set({ items: [], isUsingFallback: false, fallbackSource: null,
                cartLoadError: null, hasLegacyEmptyCart: false });
        }
        set({ cartOwnerId: owner });
        const snapshot = cartFallback.getSnapshot();
        const memorySource = get().isUsingFallback ? get().fallbackSource : null;
        const source = memorySource ?? snapshot?.source ?? null;
        const fallbackItems = memorySource ? get().items : snapshot?.items;
        const legacyEmpty = source === 'legacy-empty';
        const recoverLegacy = legacyEmpty && options.recoverLegacyEmpty === true;
        if (source === 'local-edit' || (legacyEmpty && !recoverLegacy)) {
            set({ items: fallbackItems ?? [], isLoading: false, isUsingFallback: true,
                fallbackSource: source, hasLegacyEmptyCart: legacyEmpty, cartLoadError: null });
            return;
        }

        if (fallbackItems) set({ items: fallbackItems });
        set({ isLoading: get().items.length === 0, cartLoadError: null });
        const isCurrent = () => sequence === fetchSequence && revision === cartRevision
            && currentCartOwner() === owner;
        try {
            const response = await CustomerService.getCart();
            if (!isCurrent()) return;
            if (response.success) {
                const serverItems = (response.data?.items || []).map(mapCartItemDto);
                const pendingOnlineCart = onlineCartRecovery.get();
                const items = serverItems.length > 0
                    ? serverItems
                    : pendingOnlineCart?.items ?? [];
                cartFallback.clear();
                set({ items, isLoading: false, isUsingFallback: false, fallbackSource: null,
                    cartLoadError: null, hasLegacyEmptyCart: false });
                return;
            }
            throw new Error(response.message || 'Không thể tải giỏ hàng. Vui lòng thử lại.');
        } catch (error) {
            if (!isCurrent()) return;
            if (isAuthError(error)) {
                set({ isLoading: false });
                return;
            }

            // A failed read is not evidence of an empty cart. Keep local edits intact.
            if (!recoverLegacy) cartFallback.save(get().items, 'read-cache');
            set({ isLoading: false, isUsingFallback: true,
                fallbackSource: recoverLegacy ? 'legacy-empty' : 'read-cache',
                hasLegacyEmptyCart: recoverLegacy,
                cartLoadError: getErrorMessage(error, 'Không thể tải giỏ hàng. Vui lòng thử lại.') });
        } finally {
            if (sequence === fetchSequence && revision === cartRevision) {
                if (currentCartOwner() !== owner) {
                    set({ items: [], isUsingFallback: false, fallbackSource: null,
                        cartOwnerId: null, cartLoadError: null, hasLegacyEmptyCart: false });
                }
                set({ isLoading: false });
            }
        }
    },

    hydrateProducts: async () => {
        const currentItems = get().items;
        if (currentItems.length === 0 || get().isHydratingProducts) return;
        const sequence = ++hydrationSequence;
        const revision = cartRevision;
        const owner = currentCartOwner();
        const isCurrent = () => sequence === hydrationSequence && revision === cartRevision
            && owner === currentCartOwner() && get().items === currentItems;
        set({ isHydratingProducts: true, hydrationError: null });
        try {
            const items = await Promise.all(currentItems.map(async (item) => {
                const detail = await PublicCatalogService.getProductDetail(item.product.id);
                return {
                    product: {
                        ...item.product,
                        ...mapDetailToProductModel(detail)
                    },
                    quantity: item.quantity
                };
            }));
            if (isCurrent()) set({ items, isHydratingProducts: false, hydrationError: null });
        } catch (error) {
            if (isCurrent()) set({
                isHydratingProducts: false,
                hydrationError: getErrorMessage(
                    error,
                    'Không thể cập nhật thông tin sản phẩm. Vui lòng thử lại.'
                )
            });
        } finally {
            if (sequence === hydrationSequence) set({ isHydratingProducts: false });
        }
    },

    addToCart: async (product, quantity = 1) => {
        prepareCartMutation();
        const addToFallback = () => {
            const items = [
                ...get().items.filter(item => item.product.id !== product.id),
                { product, quantity }
            ];
            onlineCartRecovery.clear();
            cartFallback.save(items);
            set({ items, isUsingFallback: true, fallbackSource: 'local-edit' });
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
        prepareCartMutation();
        const previousItems = get().items;
        const items = previousItems.filter(item => item.product.id !== productId);
        set({ items });

        const removeFromFallback = () => {
            onlineCartRecovery.clear();
            cartFallback.save(items);
            set({ isUsingFallback: true, fallbackSource: 'local-edit' });
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
        prepareCartMutation();
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
            set({ isUsingFallback: true, fallbackSource: 'local-edit' });
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
        prepareCartMutation();
        const previousItems = get().items;
        set({ items: [] });

        const clearFallback = () => {
            onlineCartRecovery.clear();
            cartFallback.save([]);
            set({ isUsingFallback: true, fallbackSource: 'local-edit' });
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
            details.customerWardName
        ].every((name) => name.trim().length > 0) && [
            details.customerCityId,
            details.customerWardId
        ].every((id) => Number.isInteger(id) && id > 0);

        if (!hasValidShippingLocation) {
            const message = 'Vui lòng chọn đầy đủ tỉnh/thành phố và phường/xã.';
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

        const nhanhEnabled = useIntegrationStore.getState().nhanhEnabled;
        if (!nhanhEnabled && items.some(({ product }) => {
            const id = Number(product.id);
            return !Number.isInteger(id) || id <= 0;
        })) {
            const message = 'Giỏ hàng có sản phẩm không hợp lệ. Vui lòng tải lại trang.';
            set({ checkoutError: message });
            throw new Error(message);
        }

        const payload: CreateNormalOrderDto = {
            ...details,
            items: items.map(({ product, quantity }) => ({
                ...(!nhanhEnabled
                    ? { productId: Number(product.id) }
                    : { nhanhProductId: product.nhanhProductId || product.id }),
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

        let createdOrder: OrderResponseDto;
        try {
            const response = await CustomerService.createOrder(payload, idempotencyKey);
            if (!response.success) {
                throw new Error(response.message || 'Không thể tạo đơn hàng.');
            }
            createdOrder = response.data;
        } catch (error) {
            const message = getErrorMessage(error, 'Không thể tạo đơn hàng. Vui lòng thử lại.');
            set({ isSubmitting: false, checkoutError: message });
            throw error;
        }

        // Order creation is the business result; cart cleanup must not undo it.
        set({ lastCreatedOrder: createdOrder, checkoutError: null,
            pendingOrderKey: null, pendingOrderFingerprint: null });
        let cleanupFailed = false;
        let preserveEmptyFallback = false;
        if (shouldClearCart) {
            try {
                prepareCartMutation();
                preserveEmptyFallback = get().isUsingFallback;
                if (!preserveEmptyFallback) {
                    const cleanup = await CustomerService.clearCart();
                    cleanupFailed = !cleanup.success;
                }
            } catch {
                cleanupFailed = true;
            }
            // Offline checkout also has no confirmed server deletion. Do not resurrect
            // an older server cart the next time the customer opens this page.
            preserveEmptyFallback = preserveEmptyFallback || cleanupFailed;
            if (preserveEmptyFallback) cartFallback.save([], 'local-edit');
            else cartFallback.clear();
            try {
                onlineCartRecovery.clear();
            } catch {
                // Browser storage can be unavailable; the order is still successful.
            }
        }

        set({
                ...(shouldClearCart ? { items: [] } : {}),
                ...(shouldClearCart ? { isUsingFallback: preserveEmptyFallback,
                    fallbackSource: preserveEmptyFallback ? 'local-edit' : null } : {}),
                isSubmitting: false,
                checkoutError: null,
                lastCreatedOrder: createdOrder,
                pendingOrderKey: null,
                pendingOrderFingerprint: null
        });
        if (cleanupFailed) {
            ToastService.warning('Đơn hàng đã được tạo. Giỏ hàng trên máy chủ chưa được cập nhật.');
        }
        return createdOrder;
    },

    restorePendingOnlineCart: () => {
        const pendingOnlineCart = onlineCartRecovery.get();
        if (!pendingOnlineCart) {
            return false;
        }
        prepareCartMutation();
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
    });
});
