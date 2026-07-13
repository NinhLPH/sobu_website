import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { CustomerService } from '../service/custom.service';
import { usePaymentStore } from './usePaymentStore';

jest.mock('../service/custom.service');

const mockedCustomerService = jest.mocked(CustomerService);

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

describe('usePaymentStore', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        sessionStorage.clear();
        usePaymentStore.setState({
            payments: [],
            activeOrderId: null,
            isLoadingPayments: false,
            isCreatingPayment: false,
            paymentError: null,
            pendingPaymentKey: null,
            pendingPaymentFingerprint: null
        });
    });

    it('loads payment history for an order', async () => {
        mockedCustomerService.getOrderPayments.mockResolvedValue({
            success: true,
            message: 'Payments retrieved',
            data: [payment]
        });

        const result = await usePaymentStore.getState().fetchPayments(12);

        expect(mockedCustomerService.getOrderPayments).toHaveBeenCalledWith(12);
        expect(result).toEqual([payment]);
        expect(usePaymentStore.getState().payments).toEqual([payment]);
        expect(usePaymentStore.getState().isLoadingPayments).toBe(false);
    });

    it('creates an online payment and leaves redirect context to the navigation helper', async () => {
        mockedCustomerService.createOrderPayment.mockResolvedValue({
            success: true,
            statusCode: 201,
            message: 'Payment created',
            data: payment
        });

        const result = await usePaymentStore.getState().createPayment(12, {
            type: 'FULL',
            paymentMethod: 'ONLINE'
        });

        expect(mockedCustomerService.createOrderPayment).toHaveBeenCalledWith(
            12,
            {
                type: 'FULL',
                paymentMethod: 'ONLINE'
            },
            expect.any(String)
        );
        expect(result).toEqual(payment);
        expect(usePaymentStore.getState().payments).toEqual([payment]);
        expect(sessionStorage.getItem('sobu.pendingPayment')).toBeNull();
    });

    it('reuses the idempotency key after a transport failure', async () => {
        mockedCustomerService.createOrderPayment.mockRejectedValue(
            new Error('Network unavailable')
        );
        const payload = {
            type: 'FULL' as const,
            paymentMethod: 'ONLINE' as const
        };

        await expect(
            usePaymentStore.getState().createPayment(12, payload)
        ).rejects.toThrow('Network unavailable');
        const firstKey = mockedCustomerService.createOrderPayment.mock.calls[0][2];

        await expect(
            usePaymentStore.getState().createPayment(12, payload)
        ).rejects.toThrow('Network unavailable');
        const secondKey = mockedCustomerService.createOrderPayment.mock.calls[1][2];

        expect(secondKey).toBe(firstKey);
        expect(usePaymentStore.getState().isCreatingPayment).toBe(false);
        expect(usePaymentStore.getState().paymentError).toBe('Network unavailable');
    });

    it('hides the raw PayOS duplicate-order error while preserving retry state', async () => {
        mockedCustomerService.createOrderPayment.mockRejectedValue(
            new Error('PayOS checkout creation failed: Đơn thanh toán đã tồn tại')
        );

        await expect(
            usePaymentStore.getState().createPayment(12, {
                type: 'FULL',
                paymentMethod: 'ONLINE'
            })
        ).rejects.toThrow('Đơn thanh toán đã tồn tại');

        expect(usePaymentStore.getState().paymentError).toBe(
            'Không thể tạo phiên thanh toán PayOS. Đơn hàng vẫn được giữ nguyên; vui lòng thử lại tại trang theo dõi đơn.'
        );
        expect(usePaymentStore.getState().pendingPaymentKey).toEqual(expect.any(String));
    });
});
