import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { AdminWorkflowService } from '../service/admin.service';
import { useAdminStore } from './useAdminStore';

jest.mock('../service/admin.service');

const mockedAdminWorkflowService = jest.mocked(AdminWorkflowService);

describe('useAdminStore order sync', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        useAdminStore.setState({
            currentOrderDetail: {
                id: 12,
                orderCode: 'OLD-CODE',
                syncStatus: 'FAILED',
                syncError: 'Old error'
            },
            adminPayments: [],
            isAdminPaymentsLoading: false,
            adminPaymentsError: null,
            isRetryingOrderSync: false,
            isCreatingFinalPayment: false,
            confirmingPaymentCode: null,
            ordersError: null,
            orderActionMessage: null
        });
    });

    it('loads order detail and persisted payment history together', async () => {
        const payment = {
            id: 30,
            orderId: 12,
            paymentCode: 'SOBU-PAY-30',
            type: 'FULL' as const,
            paymentMethod: 'ONLINE' as const,
            status: 'PENDING' as const,
            amount: 500000,
            createdAt: '2026-07-13T10:00:00',
            updatedAt: '2026-07-13T10:00:00'
        };
        mockedAdminWorkflowService.getAdminOrderDetail.mockResolvedValue({
            success: true,
            message: 'Order retrieved',
            data: { id: 12, orderCode: 'SO-12', status: 'PENDING' }
        });
        mockedAdminWorkflowService.getAdminOrderPayments.mockResolvedValue({
            success: true,
            message: 'Payments retrieved',
            data: [payment]
        });

        await useAdminStore.getState().fetchOrderDetail(12);
        const state = useAdminStore.getState();

        expect(mockedAdminWorkflowService.getAdminOrderDetail).toHaveBeenCalledWith(12);
        expect(mockedAdminWorkflowService.getAdminOrderPayments).toHaveBeenCalledWith(12);
        expect(state.currentOrderDetail?.orderCode).toBe('SO-12');
        expect(state.adminPayments).toEqual([payment]);
        expect(state.adminPaymentsError).toBeNull();
        expect(state.isAdminPaymentsLoading).toBe(false);
    });

    it('keeps order detail visible when payment history fails to load', async () => {
        mockedAdminWorkflowService.getAdminOrderDetail.mockResolvedValue({
            success: true,
            message: 'Order retrieved',
            data: { id: 12, orderCode: 'SO-12', status: 'PENDING' }
        });
        mockedAdminWorkflowService.getAdminOrderPayments.mockRejectedValue({
            response: { data: { message: 'Payment history unavailable' } }
        });

        await useAdminStore.getState().fetchOrderDetail(12);
        const state = useAdminStore.getState();

        expect(state.currentOrderDetail?.id).toBe(12);
        expect(state.adminPayments).toEqual([]);
        expect(state.adminPaymentsError).toBe('Payment history unavailable');
        expect(state.ordersError).toBeNull();
    });

    it('merges the retry result into the current order detail', async () => {
        mockedAdminWorkflowService.retryOrderSync.mockResolvedValue({
            success: true,
            message: 'Retry success',
            data: {
                orderId: 12,
                orderCode: 'SO-12',
                syncStatus: 'SYNCED',
                nhanhSyncStage: 'NORMAL_ORDER_CREATED',
                nhanhOrderId: '9001',
                nhanhOrderCode: 'NH-9001',
                lastSyncMessage: 'Synced'
            }
        });

        const result = await useAdminStore.getState().retryOrderSync(12);
        const state = useAdminStore.getState();

        expect(mockedAdminWorkflowService.retryOrderSync).toHaveBeenCalledWith(12);
        expect(result.syncStatus).toBe('SYNCED');
        expect(state.currentOrderDetail).toMatchObject({
            id: 12,
            orderCode: 'SO-12',
            syncStatus: 'SYNCED',
            nhanhSyncStage: 'NORMAL_ORDER_CREATED',
            nhanhOrderId: '9001',
            nhanhOrderCode: 'NH-9001',
            lastSyncMessage: 'Synced'
        });
        expect(state.isRetryingOrderSync).toBe(false);
        expect(state.orderActionMessage).toBe('Retry success');
    });

    it('stores the backend error and clears retry loading', async () => {
        const error = {
            response: {
                data: {
                    message: 'Retry rejected'
                }
            }
        };
        mockedAdminWorkflowService.retryOrderSync.mockRejectedValue(error);

        await expect(
            useAdminStore.getState().retryOrderSync(12)
        ).rejects.toBe(error);

        expect(useAdminStore.getState().isRetryingOrderSync).toBe(false);
        expect(useAdminStore.getState().ordersError).toBe('Retry rejected');
    });

    it('creates a final payment and refreshes the order detail', async () => {
        const finalPayment = {
            id: 31,
            orderId: 12,
            paymentCode: 'SOBU-PAY-FINAL',
            type: 'FINAL' as const,
            paymentMethod: 'ONLINE' as const,
            status: 'PENDING' as const,
            amount: 400000,
            createdAt: '2026-06-14T10:00:00',
            updatedAt: '2026-06-14T10:00:00'
        };
        mockedAdminWorkflowService.createPreorderFinalPayment.mockResolvedValue({
            success: true,
            statusCode: 201,
            message: 'Final payment created',
            data: finalPayment
        });
        mockedAdminWorkflowService.getAdminOrderDetail.mockResolvedValue({
            success: true,
            message: 'Retrieved successfully',
            data: {
                id: 12,
                type: 'PREORDER',
                status: 'READY_FOR_FINAL_PAYMENT'
            }
        });
        mockedAdminWorkflowService.getAdminOrderPayments.mockResolvedValue({
            success: true,
            message: 'Payments retrieved',
            data: [finalPayment]
        });

        const result = await useAdminStore.getState().createPreorderFinalPayment(12);
        const state = useAdminStore.getState();

        expect(result).toEqual(finalPayment);
        expect(state.adminPayments).toEqual([finalPayment]);
        expect(state.currentOrderDetail?.status).toBe('READY_FOR_FINAL_PAYMENT');
        expect(state.isCreatingFinalPayment).toBe(false);
        expect(state.orderActionMessage).toBe('Final payment created');
        expect(mockedAdminWorkflowService.getAdminOrderPayments).toHaveBeenCalledWith(12);
    });

    it('confirms a mock payment and replaces it with the paid response', async () => {
        const pendingPayment = {
            id: 32,
            orderId: 12,
            paymentCode: 'SOBU-PAY-MOCK',
            type: 'FULL' as const,
            paymentMethod: 'ONLINE' as const,
            status: 'PENDING' as const,
            amount: 500000,
            createdAt: '2026-06-14T10:00:00',
            updatedAt: '2026-06-14T10:00:00'
        };
        useAdminStore.setState({ adminPayments: [pendingPayment] });
        mockedAdminWorkflowService.confirmMockPayment.mockResolvedValue({
            success: true,
            message: 'Payment confirmed',
            data: {
                ...pendingPayment,
                status: 'PAID',
                paidAt: '2026-06-14T10:05:00'
            }
        });
        mockedAdminWorkflowService.getAdminOrderDetail.mockResolvedValue({
            success: true,
            message: 'Retrieved successfully',
            data: {
                id: 12,
                paymentStatus: 'PAID',
                paidAmount: 500000,
                remainingAmount: 0
            }
        });
        mockedAdminWorkflowService.getAdminOrderPayments.mockResolvedValue({
            success: true,
            message: 'Payments retrieved',
            data: [{
                ...pendingPayment,
                status: 'PAID',
                paidAt: '2026-06-14T10:05:00'
            }]
        });

        await useAdminStore.getState().confirmMockPayment(' SOBU-PAY-MOCK ');
        const state = useAdminStore.getState();

        expect(mockedAdminWorkflowService.confirmMockPayment)
            .toHaveBeenCalledWith('SOBU-PAY-MOCK');
        expect(state.adminPayments).toHaveLength(1);
        expect(state.adminPayments[0].status).toBe('PAID');
        expect(state.currentOrderDetail?.paymentStatus).toBe('PAID');
        expect(state.confirmingPaymentCode).toBeNull();
        expect(mockedAdminWorkflowService.getAdminOrderPayments).toHaveBeenCalledWith(12);
    });
});
