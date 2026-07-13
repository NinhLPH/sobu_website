import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import OrderTracking, { getAvailablePaymentTypes } from './OrderTracking';
import { CustomerService } from '../service/custom.service';
import { usePaymentStore } from '../store/usePaymentStore';
import { redirectToPaymentCheckout } from '../utils/payment-session';
import { CreateOrderPaymentDto, OrderPaymentResponseDto } from '../interface/order.model';

let mockSearchQuery = 'nhanhOrderId=NH-42';

jest.mock('react-router-dom', () => ({
    Link: ({ children, to }: { children: React.ReactNode; to: string }) => (
        <a href={to}>{children}</a>
    ),
    useSearchParams: () => [new URLSearchParams(mockSearchQuery)],
    useParams: () => ({})
}), { virtual: true });
jest.mock('../service/custom.service');
jest.mock('../store/usePaymentStore');
jest.mock('../utils/payment-session', () => ({
    redirectToPaymentCheckout: require('@jest/globals').jest.fn()
}));

const mockedCustomerService = jest.mocked(CustomerService);
const mockedUsePaymentStore = jest.mocked(usePaymentStore);
const mockedRedirectToPaymentCheckout = jest.mocked(redirectToPaymentCheckout);
const fetchPayments = jest.fn(async () => []);
const createPayment = jest.fn<Promise<OrderPaymentResponseDto>, [string | number, CreateOrderPaymentDto]>();
const clearPaymentError = jest.fn();
const clearPayments = jest.fn();

beforeEach(() => {
    jest.clearAllMocks();
    mockSearchQuery = 'nhanhOrderId=NH-42';
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

    it('shows a safe recovery action after checkout setup fails and retries the existing order', async () => {
        mockSearchQuery = 'orderId=42&paymentSetup=failed';
        const retryPayment = {
            id: 88,
            orderId: 42,
            paymentCode: 'SOBU-PAY-RETRY',
            type: 'FULL' as const,
            paymentMethod: 'ONLINE' as const,
            status: 'PENDING' as const,
            amount: 500000,
            checkoutUrl: 'https://pay.payos.vn/web/checkout/retry',
            createdAt: '2026-07-13T10:00:00',
            updatedAt: '2026-07-13T10:00:00'
        };
        mockedCustomerService.getMyOrder.mockResolvedValue({
            success: true,
            message: 'OK',
            data: {
                id: 42,
                orderCode: 'SO-42',
                type: 'NORMAL',
                status: 'PENDING',
                paymentStatus: 'PENDING',
                totalAmount: 500000,
                items: []
            }
        });
        createPayment.mockResolvedValue(retryPayment);
        mockedUsePaymentStore.mockReturnValue({
            payments: [{
                ...retryPayment,
                id: 87,
                status: 'FAILED',
                checkoutUrl: undefined
            }],
            isLoadingPayments: false,
            isCreatingPayment: false,
            paymentError: null,
            fetchPayments,
            createPayment,
            clearPaymentError,
            clearPayments
        } as ReturnType<typeof usePaymentStore>);

        render(<OrderTracking />);

        await waitFor(() => expect(screen.getByText('Phiên thanh toán chưa được tạo')).toBeTruthy());
        fireEvent.click(screen.getByRole('button', { name: 'Thử tạo lại thanh toán' }));

        await waitFor(() => expect(createPayment).toHaveBeenCalledWith(42, {
            type: 'FULL',
            paymentMethod: 'ONLINE'
        }));
        expect(mockedRedirectToPaymentCheckout).toHaveBeenCalledWith(retryPayment);
    });

    it('reconciles a cancelled order when the cancel response fails after commit', async () => {
        mockSearchQuery = 'orderId=42';
        mockedCustomerService.getMyOrder
            .mockResolvedValueOnce({
                success: true,
                message: 'OK',
                data: {
                    id: 42,
                    orderCode: 'SO-42',
                    type: 'NORMAL',
                    status: 'PENDING',
                    paymentStatus: 'PENDING',
                    totalAmount: 500000,
                    items: []
                }
            })
            .mockResolvedValueOnce({
                success: true,
                message: 'OK',
                data: {
                    id: 42,
                    orderCode: 'SO-42',
                    type: 'NORMAL',
                    status: 'CANCELLED',
                    paymentStatus: 'PENDING',
                    totalAmount: 500000,
                    items: []
                }
            });
        mockedCustomerService.cancelOrder.mockRejectedValue(new Error('Server returned 500'));
        jest.spyOn(window, 'confirm').mockReturnValue(true);

        render(<OrderTracking />);

        await waitFor(() => expect(screen.getByRole('button', { name: 'Hủy đơn' })).toBeTruthy());
        fireEvent.click(screen.getByRole('button', { name: 'Hủy đơn' }));

        await waitFor(() => expect(screen.getByText('Đơn hàng đã được hủy.')).toBeTruthy());
        expect(mockedCustomerService.cancelOrder).toHaveBeenCalledTimes(1);
        expect(mockedCustomerService.getMyOrder).toHaveBeenCalledTimes(2);
        expect((screen.getByRole('button', { name: 'Đã hủy đơn' }) as HTMLButtonElement).disabled).toBe(true);
    });
});
