import { CartItem } from '../interface/product.model';
import { authStorage } from './auth-storage';

const CART_FALLBACK_KEY_PREFIX = 'sobu.cartFallback.v1';

export type CartFallbackSource = 'read-cache' | 'local-edit' | 'legacy-empty';

export interface CartFallbackSnapshot {
    items: CartItem[];
    source: CartFallbackSource;
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
        return cartFallback.getSnapshot()?.items ?? null;
    },

    getSnapshot: (): CartFallbackSnapshot | null => {
        try {
            const key = getStorageKey();
            if (!key || typeof window === 'undefined') return null;
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

            const source = snapshot.source === 'read-cache' || snapshot.source === 'local-edit'
                ? snapshot.source
                : items.length > 0 ? 'local-edit' : 'legacy-empty';
            return { items: cloneItems(items), source };
        } catch {
            return null;
        }
    },

    save: (items: CartItem[], source: Exclude<CartFallbackSource, 'legacy-empty'> = 'local-edit'): boolean => {
        try {
            const key = getStorageKey();
            if (!key || typeof window === 'undefined') return false;
            const snapshot: CartFallbackSnapshot = { items: cloneItems(items), source };
            window.sessionStorage.setItem(key, JSON.stringify(snapshot));
            return true;
        } catch {
            return false;
        }
    },

    clear: (): void => {
        try {
            const key = getStorageKey();
            if (!key || typeof window === 'undefined') return;
            window.sessionStorage.removeItem(key);
        } catch {
            // Storage access can be disabled by the browser; keep the in-memory cart usable.
        }
    }
};
