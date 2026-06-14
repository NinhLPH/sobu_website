import { OrderPaymentResponseDto } from '../interface/order.model';

const PAYMENT_CONTEXT_KEY = 'sobu.pendingPayment';

export interface PendingPaymentContext {
    orderId: string;
    paymentCode: string;
}

export const paymentSession = {
    save: (payment: OrderPaymentResponseDto) => {
        if (typeof window === 'undefined') {
            return;
        }
        const context: PendingPaymentContext = {
            orderId: String(payment.orderId),
            paymentCode: payment.paymentCode
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
            return JSON.parse(value) as PendingPaymentContext;
        } catch {
            window.sessionStorage.removeItem(PAYMENT_CONTEXT_KEY);
            return null;
        }
    }
};
