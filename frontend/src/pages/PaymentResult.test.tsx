import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import PaymentResult from './PaymentResult';
import { usePaymentStore } from '../store/usePaymentStore';
import { useCartStore } from '../store/useCartStore';

let mockQuery = 'orderId=12&paymentCode=SOBU-PAY-001';

jest.mock('react-router-dom', () => ({
    Link: ({ children, to }: { children: React.ReactNode; to: string }) => (
        <a href={to}>{children}</a>
    ),
    useSearchParams: () => [new URLSearchParams(mockQuery)]
}), { virtual: true });
jest.mock('../store/usePaymentStore');
jest.mock('../store/useCartStore');

const mockedUsePaymentStore = jest.mocked(usePaymentStore);
const mockedUseCartStore = jest.mocked(useCartStore);
const fetchPayments = jest.fn<Promise<any>, any[]>();
const clearPaymentError = jest.fn();
const restorePendingOnlineCart = jest.fn();

describe('PaymentResult', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        sessionStorage.clear();
        mockQuery = 'orderId=12&paymentCode=SOBU-PAY-001';
        mockedUsePaymentStore.mockReturnValue({
            fetchPayments,
            paymentError: null,
            clearPaymentError
        } as ReturnType<typeof usePaymentStore>);
        mockedUseCartStore.mockImplementation((selector: any) => selector({
            restorePendingOnlineCart
        }));
    });

    it('does not trust a PayOS code=00 without a matching paid backend record', async () => {
        mockQuery += '&code=00';
        fetchPayments.mockResolvedValue([]);

        const view = render(<PaymentResult />);

        await waitFor(() => expect(fetchPayments).toHaveBeenCalledWith('12'));
        expect(screen.queryByText('Thanh toán thành công')).toBeNull();
        expect(screen.getByText('Đang xác nhận giao dịch')).toBeTruthy();
        view.unmount();
    });

    it('ignores a paid record with a different payment code', async () => {
        fetchPayments.mockResolvedValue([{
            id: 22,
            orderId: 12,
            paymentCode: 'SOBU-PAY-OTHER',
            type: 'FULL',
            paymentMethod: 'ONLINE',
            status: 'PAID',
            amount: 500000,
            createdAt: '2026-06-14T10:00:00',
            updatedAt: '2026-06-14T10:05:00'
        }]);

        const view = render(<PaymentResult />);

        await waitFor(() => expect(fetchPayments).toHaveBeenCalledWith('12'));
        expect(screen.queryByText('Thanh toán thành công')).toBeNull();
        view.unmount();
    });

    it('shows success and clears session only after the exact payment is paid', async () => {
        sessionStorage.setItem('sobu.pendingPayment', JSON.stringify({
            orderId: '12',
            paymentCode: 'SOBU-PAY-001',
            createdAt: Date.now()
        }));
        fetchPayments.mockResolvedValue([{
            id: 21,
            orderId: 12,
            paymentCode: 'SOBU-PAY-001',
            type: 'FULL',
            paymentMethod: 'ONLINE',
            status: 'PAID',
            amount: 500000,
            createdAt: '2026-06-14T10:00:00',
            updatedAt: '2026-06-14T10:05:00'
        }]);

        render(<PaymentResult />);

        await waitFor(() => expect(screen.getByText('Thanh toán thành công')).toBeTruthy());
        expect(sessionStorage.getItem('sobu.pendingPayment')).toBeNull();
    });

    it('restores a pending online cart snapshot on mount', async () => {
        fetchPayments.mockResolvedValue([]);

        const view = render(<PaymentResult />);

        await waitFor(() => expect(restorePendingOnlineCart).toHaveBeenCalled());
        view.unmount();
    });
});
