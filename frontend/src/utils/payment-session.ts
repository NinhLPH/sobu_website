import { OrderPaymentResponseDto } from '../interface/order.model';

const PAYMENT_CONTEXT_KEY = 'sobu.pendingPayment';
const PAYMENT_CONTEXT_MAX_AGE_MS = 30 * 60 * 1000;

export interface PendingPaymentContext {
    orderId: string;
    paymentCode: string;
    createdAt: number;
}

export const paymentSession = {
    save: (payment: OrderPaymentResponseDto) => {
        if (typeof window === 'undefined') {
            return;
        }
        const context: PendingPaymentContext = {
            orderId: String(payment.orderId),
            paymentCode: payment.paymentCode,
            createdAt: Date.now()
        };
        window.sessionStorage.setItem(PAYMENT_CONTEXT_KEY, JSON.stringify(context));
    },

    get: (): PendingPaymentContext | null => {
        if (typeof window === 'undefined') {
            return null;
        }
        const value = window.sessionStorage.getItem(PAYMENT_CONTEXT_KEY);
        if (!value) {
            return null;
        }
        try {
            const context = JSON.parse(value) as Partial<PendingPaymentContext>;
            if (
                typeof context.orderId !== 'string' ||
                typeof context.paymentCode !== 'string' ||
                typeof context.createdAt !== 'number' ||
                Date.now() - context.createdAt > PAYMENT_CONTEXT_MAX_AGE_MS
            ) {
                window.sessionStorage.removeItem(PAYMENT_CONTEXT_KEY);
                return null;
            }
            return context as PendingPaymentContext;
        } catch {
            window.sessionStorage.removeItem(PAYMENT_CONTEXT_KEY);
            return null;
        }
    },

    clear: () => {
        if (typeof window === 'undefined') {
            return;
        }
        window.sessionStorage.removeItem(PAYMENT_CONTEXT_KEY);
    }
};

export const redirectToPaymentCheckout = (payment: OrderPaymentResponseDto) => {
    if (!payment.checkoutUrl) {
        throw new Error('Cổng thanh toán chưa trả về đường dẫn checkout.');
    }
    paymentSession.save(payment);
    window.location.assign(payment.checkoutUrl);
};
