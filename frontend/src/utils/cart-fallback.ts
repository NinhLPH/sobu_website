import { CartItem } from '../interface/product.model';
import { authStorage } from './auth-storage';

const CART_FALLBACK_KEY_PREFIX = 'sobu.cartFallback.v1';

interface CartFallbackSnapshot {
    items: CartItem[];
}

const cloneItems = (items: CartItem[]): CartItem[] =>
    items.map(({ product, quantity }) => ({
        product: { ...product },
        quantity
    }));

const getStorageKey = (): string | null => {
    const user = authStorage.getUser();
    return user ? `${CART_FALLBACK_KEY_PREFIX}:${user.id}` : null;
};

const isCartItem = (item: Partial<CartItem> | null | undefined): item is CartItem =>
    Boolean(
        item &&
        item.product &&
        typeof item.quantity === 'number' &&
        Number.isFinite(item.quantity) &&
        item.quantity > 0
    );

export const cartFallback = {
    get: (): CartItem[] | null => {
        const key = getStorageKey();
        if (!key || typeof window === 'undefined') {
            return null;
        }

        try {
            const value = window.sessionStorage.getItem(key);
            if (!value) {
                return null;
            }

            const snapshot = JSON.parse(value) as Partial<CartFallbackSnapshot>;
            if (!Array.isArray(snapshot.items)) {
                window.sessionStorage.removeItem(key);
                return null;
            }

            const items = snapshot.items.filter(isCartItem);
            if (items.length !== snapshot.items.length) {
                window.sessionStorage.removeItem(key);
                return null;
            }

            return cloneItems(items);
        } catch {
            return null;
        }
    },

    save: (items: CartItem[]): boolean => {
        const key = getStorageKey();
        if (!key || typeof window === 'undefined') {
            return false;
        }

        try {
            const snapshot: CartFallbackSnapshot = { items: cloneItems(items) };
            window.sessionStorage.setItem(key, JSON.stringify(snapshot));
            return true;
        } catch {
            return false;
        }
    },

    clear: (): void => {
        const key = getStorageKey();
        if (!key || typeof window === 'undefined') {
            return;
        }

        try {
            window.sessionStorage.removeItem(key);
        } catch {
            // Storage access can be disabled by the browser; keep the in-memory cart usable.
        }
    }
};
