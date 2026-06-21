import { render, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import OrderTracking, { getAvailablePaymentTypes } from './OrderTracking';
import { CustomerService } from '../service/custom.service';
import { usePaymentStore } from '../store/usePaymentStore';

jest.mock('react-router-dom', () => ({
    Link: ({ children, to }: { children: React.ReactNode; to: string }) => (
        <a href={to}>{children}</a>
    ),
    useSearchParams: () => [new URLSearchParams('nhanhOrderId=NH-42')]
}), { virtual: true });
jest.mock('../service/custom.service');
jest.mock('../store/usePaymentStore');

const mockedCustomerService = jest.mocked(CustomerService);
const mockedUsePaymentStore = jest.mocked(usePaymentStore);
const fetchPayments = jest.fn(async () => []);
const createPayment = jest.fn();
const clearPaymentError = jest.fn();
const clearPayments = jest.fn();

beforeEach(() => {
    jest.clearAllMocks();
    mockedUsePaymentStore.mockReturnValue({
        payments: [],
        isLoadingPayments: false,
        isCreatingPayment: false,
        paymentError: null,
        fetchPayments,
        createPayment,
        clearPaymentError,
        clearPayments
    } as ReturnType<typeof usePaymentStore>);
});

describe('OrderTracking workflow', () => {
    it('opens an order directly from a nhanhOrderId query parameter', async () => {
        mockedCustomerService.getOrderByNhanhId.mockResolvedValue({
            success: true,
            message: 'OK',
            data: {
                id: 42,
                orderCode: 'SO-42',
                nhanhOrderId: 'NH-42',
                type: 'PREORDER',
                status: 'WAITING_DEPOSIT',
                paymentStatus: 'PENDING',
                totalAmount: 1500000,
                depositAmount: 300000,
                items: []
            }
        });

        render(<OrderTracking />);

        await waitFor(() => {
            expect(mockedCustomerService.getOrderByNhanhId).toHaveBeenCalledWith('NH-42');
            expect(fetchPayments).toHaveBeenCalledWith(42);
        });
        expect(mockedCustomerService.getMyOrder).not.toHaveBeenCalled();
    });

    it('gates customer payment types to backend-supported order states', () => {
        expect(getAvailablePaymentTypes({
            id: 1,
            type: 'NORMAL',
            status: 'PENDING',
            paymentStatus: 'PENDING'
        })).toEqual(['FULL']);
        expect(getAvailablePaymentTypes({
            id: 2,
            type: 'PREORDER',
            status: 'WAITING_DEPOSIT',
            paymentStatus: 'PENDING'
        })).toEqual(['DEPOSIT']);
        expect(getAvailablePaymentTypes({
            id: 3,
            type: 'PREORDER',
            status: 'READY_FOR_FINAL_PAYMENT',
            paymentStatus: 'PENDING'
        })).toEqual(['FINAL']);
        expect(getAvailablePaymentTypes({
            id: 4,
            type: 'CUSTOM',
            status: 'PENDING',
            paymentStatus: 'PENDING'
        })).toEqual([]);
        expect(getAvailablePaymentTypes({
            id: 5,
            type: 'FINDING',
            status: 'PENDING',
            paymentStatus: 'PENDING'
        })).toEqual([]);
    });
});
