import { OrderPaymentResponseDto } from '../interface/order.model';
import { CartItem } from '../interface/product.model';

const ONLINE_CART_RECOVERY_KEY = 'sobu.pendingOnlineCart';
const ONLINE_CART_RECOVERY_MAX_AGE_MS = 30 * 60 * 1000;

export interface PendingOnlineCartRecovery {
    orderId: string;
    paymentCode: string;
    items: CartItem[];
    createdAt: number;
}

const cloneItems = (items: CartItem[]): CartItem[] =>
    items.map(({ product, quantity }) => ({
        product: { ...product },
        quantity
    }));

const isValidCartItem = (item: Partial<CartItem> | null | undefined): item is CartItem =>
    Boolean(
        item &&
        item.product &&
        typeof item.product.id === 'string' &&
        typeof item.product.name === 'string' &&
        typeof item.product.price === 'number' &&
        typeof item.quantity === 'number' &&
        item.quantity > 0
    );

export const onlineCartRecovery = {
    save: (payment: OrderPaymentResponseDto, items: CartItem[]) => {
        if (typeof window === 'undefined' || payment.paymentMethod !== 'ONLINE' || items.length === 0) {
            return;
        }

        const context: PendingOnlineCartRecovery = {
            orderId: String(payment.orderId),
            paymentCode: payment.paymentCode,
            items: cloneItems(items),
            createdAt: Date.now()
        };
        window.sessionStorage.setItem(ONLINE_CART_RECOVERY_KEY, JSON.stringify(context));
    },

    get: (): PendingOnlineCartRecovery | null => {
        if (typeof window === 'undefined') {
            return null;
        }

        const value = window.sessionStorage.getItem(ONLINE_CART_RECOVERY_KEY);
        if (!value) {
            return null;
        }

        try {
            const context = JSON.parse(value) as Partial<PendingOnlineCartRecovery>;
            const items = Array.isArray(context.items)
                ? context.items.filter(isValidCartItem)
                : [];
            if (
                typeof context.orderId !== 'string' ||
                typeof context.paymentCode !== 'string' ||
                typeof context.createdAt !== 'number' ||
                Date.now() - context.createdAt > ONLINE_CART_RECOVERY_MAX_AGE_MS ||
                items.length === 0
            ) {
                window.sessionStorage.removeItem(ONLINE_CART_RECOVERY_KEY);
                return null;
            }

            return {
                orderId: context.orderId,
                paymentCode: context.paymentCode,
                items: cloneItems(items),
                createdAt: context.createdAt
            };
        } catch {
            window.sessionStorage.removeItem(ONLINE_CART_RECOVERY_KEY);
            return null;
        }
    },

    clear: () => {
        if (typeof window === 'undefined') {
            return;
        }
        window.sessionStorage.removeItem(ONLINE_CART_RECOVERY_KEY);
    }
};
