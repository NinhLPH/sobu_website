import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { paymentSession } from './payment-session';

const payment = {
    id: 21,
    orderId: 12,
    paymentCode: 'SOBU-PAY-001',
    type: 'FULL' as const,
    paymentMethod: 'ONLINE' as const,
    status: 'PENDING' as const,
    amount: 500000,
    checkoutUrl: 'https://pay.payos.vn/web/checkout',
    createdAt: '2026-06-14T10:00:00',
    updatedAt: '2026-06-14T10:00:00'
};

describe('paymentSession', () => {
    beforeEach(() => {
        sessionStorage.clear();
        jest.restoreAllMocks();
    });

    it('stores redirect context with a creation timestamp', () => {
        jest.spyOn(Date, 'now').mockReturnValue(1_750_000_000_000);

        paymentSession.save(payment);

        expect(paymentSession.get()).toEqual({
            orderId: '12',
            paymentCode: 'SOBU-PAY-001',
            createdAt: 1_750_000_000_000
        });
    });

    it('discards context older than thirty minutes', () => {
        jest.spyOn(Date, 'now').mockReturnValue(1_750_000_000_000);
        sessionStorage.setItem('sobu.pendingPayment', JSON.stringify({
            orderId: '12',
            paymentCode: 'SOBU-PAY-001',
            createdAt: 1_750_000_000_000 - 30 * 60 * 1000 - 1
        }));

        expect(paymentSession.get()).toBeNull();
        expect(sessionStorage.getItem('sobu.pendingPayment')).toBeNull();
    });

    it('clears terminal redirect context', () => {
        paymentSession.save(payment);
        paymentSession.clear();

        expect(paymentSession.get()).toBeNull();
    });
});
