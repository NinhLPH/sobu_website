import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import AdminOrderDetail from './OrderDetails';
import { useAdminStore } from '../../store/useAdminStore';

jest.mock('react-router-dom', () => ({
    Link: ({ children, to }: any) => <a href={to}>{children}</a>,
    useParams: () => ({ id: '12' })
}), { virtual: true });
jest.mock('../../store/useAdminStore');

const mockedUseAdminStore = jest.mocked(useAdminStore);
const fetchOrderDetail = jest.fn(async () => undefined);
const fetchOrderPayments = jest.fn(async () => []);
const clearCurrentOrder = jest.fn();

const renderDetail = (overrides: Record<string, unknown> = {}) => {
    mockedUseAdminStore.mockReturnValue({
        currentOrderDetail: {
            id: 12,
            orderCode: 'SO-12',
            type: 'NORMAL',
            status: 'PENDING',
            totalAmount: 500000,
            items: []
        },
        adminPayments: [],
        fetchOrderDetail,
        fetchOrderPayments,
        retryOrderSync: jest.fn(),
        createPreorderFinalPayment: jest.fn(),
        confirmMockPayment: jest.fn(),
        clearCurrentOrder,
        clearOrdersError: jest.fn(),
        clearOrderActionMessage: jest.fn(),
        isOrderDetailLoading: false,
        isAdminPaymentsLoading: false,
        adminPaymentsError: null,
        retryingOrderIds: [],
        isCreatingFinalPayment: false,
        confirmingPaymentCode: null,
        ordersError: null,
        orderActionMessage: null,
        ...overrides
    } as ReturnType<typeof useAdminStore>);

    render(<AdminOrderDetail />);
};

beforeEach(() => {
    jest.clearAllMocks();
});

describe('AdminOrderDetail payment history', () => {
    it('shows persisted payment count instead of a session-only count', () => {
        renderDetail({
            adminPayments: [{
                id: 30,
                orderId: 12,
                paymentCode: 'SOBU-PAY-30',
                type: 'FULL',
                paymentMethod: 'ONLINE',
                status: 'PENDING',
                amount: 500000
            }]
        });

        expect(screen.getByText('1 giao dịch đã ghi nhận')).toBeTruthy();
        expect(screen.getByText(/SOBU-PAY-30/)).toBeTruthy();
    });

    it('shows a payment-specific error and retries without hiding the order', () => {
        renderDetail({ adminPaymentsError: 'Không thể tải payment.' });

        expect(screen.getByText('Chi tiết đơn #SO-12')).toBeTruthy();
        expect(screen.getByText('Không thể tải payment.')).toBeTruthy();
        fireEvent.click(screen.getByRole('button', { name: 'Thử lại' }));
        expect(fetchOrderPayments).toHaveBeenCalledWith('12');
    });
});
